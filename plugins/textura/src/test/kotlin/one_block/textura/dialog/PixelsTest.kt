package one_block.textura.dialog

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextColor
import one_block.textura.api.DialogBinds
import one_block.textura.dialog.json.ScreenCompiler
import one_block.textura.dialog.kit.UiPiece
import one_block.textura.dialog.layout.UiLayout
import one_block.textura.dialog.render.PixelRun
import one_block.textura.dialog.render.PixelRuns
import one_block.textura.dialog.render.UiRenderer
import net.kyori.adventure.text.event.ClickEvent
import java.awt.image.BufferedImage

private const val RED = 0xFFFF0000.toInt()
private const val BLUE = 0xFF0000FF.toInt()
private const val CLEAR = 0x00000000

class PixelsTest : StringSpec({

    "linha junta cores iguais e pula transparente" {
        PixelRuns.of(intArrayOf(RED, RED, RED, CLEAR, BLUE)) shouldContainExactly listOf(
            PixelRun(0, 3, 0xFF0000),
            PixelRun(4, 1, 0x0000FF),
        )
    }

    "corrida longa vira pedaços de potência de 2 do maior para o menor" {
        PixelRuns.pieces(PixelRun(2, 13, 0x00FF00), 8) shouldContainExactly listOf(
            PixelRun(2, 8, 0x00FF00),
            PixelRun(10, 4, 0x00FF00),
            PixelRun(14, 1, 0x00FF00),
        )
    }

    "vermelho do marcador do HUD nunca sai igual" {
        PixelRuns.of(intArrayOf(0xFFFE1020.toInt(), 0xFFFD3040.toInt())).map { it.rgb } shouldContainExactly listOf(0xFF1020, 0xFF3040)
    }

    val json = """
        {"theme": "stone", "column": [
          {"text": "Mapa"},
          {"pixels": "mapa", "w": 8, "h": 4, "scale": 2}
        ]}
    """.trimIndent()

    "o nó pixels desenha a imagem do código no lugar dele" {
        val (definition, warnings) = DialogFixtures.parse(json)
        warnings.shouldBeEmpty()
        val image = BufferedImage(4, 2, BufferedImage.TYPE_INT_ARGB).apply {
            setRGB(0, 0, RED)
            setRGB(1, 0, RED)
            setRGB(3, 1, BLUE)
        }
        val binds = DialogBinds().apply { pixels("mapa") { image } }
        val session = DialogSession(definition!!.id, binds, player = DialogFixtures.player())
        val kit = DialogFixtures.pixelKit
        val laid = UiLayout(kit).layout(ScreenCompiler(kit) { }.compile(definition, DialogFixtures.theme, session))

        laid.pixels.single().let { (it.x to it.y) shouldBe (0 to 9) }
        laid.height shouldBe 13

        val lines = UiRenderer(kit, { ClickEvent.runCommand("/x") }) { }.render(laid)
        val colors = lines.flatMap { flatten(it) }.mapNotNull { it.color() }
        colors shouldContainExactly listOf(TextColor.color(0xFF0000), TextColor.color(0x0000FF))
        lines[1].toString() shouldContain "image_0"
        lines[1].toString() shouldContain "image_2"
    }

    "com detalhe 3 cada pixel da GUI recebe 3x3 pontos e o deslocamento vai no glifo" {
        val json = """
            {"theme": "stone", "column": [
              {"pixels": "busto", "w": 2, "h": 1, "detail": 3}
            ]}
        """.trimIndent()
        val (definition, warnings) = DialogFixtures.parse(json)
        warnings.shouldBeEmpty()
        val image = BufferedImage(6, 3, BufferedImage.TYPE_INT_ARGB).apply {
            setRGB(4, 2, BLUE)
        }
        val binds = DialogBinds().apply { pixels("busto") { image } }
        val session = DialogSession(definition!!.id, binds, player = DialogFixtures.player())
        val kit = DialogFixtures.pixelKit
        val laid = UiLayout(kit).layout(ScreenCompiler(kit) { }.compile(definition, DialogFixtures.theme, session))

        laid.pixels.single().detail shouldBe 3
        laid.height shouldBe 1
        val line = UiRenderer(kit, { ClickEvent.runCommand("/x") }) { }.render(laid).first().toString()
        line shouldContain "d3:1:1:2"
    }

    "sem imagem do código o nó só ocupa o espaço" {
        val (definition, _) = DialogFixtures.parse(json)
        val session = DialogSession(definition!!.id, DialogBinds(), player = DialogFixtures.player())
        val kit = DialogFixtures.pixelKit
        val laid = UiLayout(kit).layout(ScreenCompiler(kit) { }.compile(definition, DialogFixtures.theme, session))

        laid.pixels.shouldBeEmpty()
        laid.height shouldBe 13
    }
}) {
    companion object {
        fun flatten(component: Component): List<TextComponent> =
            listOfNotNull(component as? TextComponent).filter { it.content().isNotEmpty() } + component.children().flatMap(::flatten)
    }
}

private val DialogFixtures.pixelKit
    get() = object : one_block.textura.dialog.kit.UiKit by DialogFixtures.kit {
        override fun pixel(scale: Int, width: Int): UiPiece = UiPiece("p$width", width * scale + 1)
        override fun detailPixel(detail: Int, width: Int, dx: Int, dy: Int): UiPiece = UiPiece("d$detail:$width:$dx:$dy", 1)
        override fun spaceChar(amount: Int): String = "s"
    }
