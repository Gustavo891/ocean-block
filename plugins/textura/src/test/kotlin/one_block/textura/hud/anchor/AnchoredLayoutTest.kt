package one_block.textura.hud.anchor

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import one_block.textura.text.SpacingFont
import one_block.textura.text.TextMetrics

class AnchoredLayoutTest : StringSpec({

    fun advance(component: Component): Int {
        val own = (component as? TextComponent)?.content()?.let { content ->
            if (component.font() == SpacingFont.KEY) {
                content.sumOf { SpacingFont.ADVANCES.getValue(it.toString()) }
            } else {
                TextMetrics.calculateWidth(content)
            }
        } ?: 0
        return own + component.children().sumOf(::advance)
    }

    "a sonda inteira tem avanço zero para a origem ficar no centro da tela" {
        advance(HudAnchorProbe.render()) shouldBe 0
    }

    "cada linha ancorada devolve o cursor à origem" {
        ScreenAnchor.entries.forEach { anchor ->
            val line = AnchorLine(anchor, 2, listOf(AnchorSpan("+1", AnchorPalette.GREEN), AnchorSpan(" Tábua", AnchorPalette.WHITE)))
            advance(AnchoredLayout.anchored(line)) shouldBe 0
        }
    }

    "ancora à direita recua a largura antes do texto" {
        val line = AnchoredLayout.anchored(AnchorLine(ScreenAnchor.TOP_RIGHT, 0, listOf(AnchorSpan("ab", AnchorPalette.WHITE))))
        advance(line.children().first()) shouldBe -TextMetrics.calculateWidth("ab")
    }

    "o marcador codifica ancora e paleta em multiplos de 16" {
        val glyph = AnchorMarker.glyph(ScreenAnchor.MIDDLE_RIGHT, AnchorPalette.GREEN)
        glyph.red() shouldBe 254
        glyph.green() shouldBe 64
        glyph.blue() shouldBe 160

        val shadow = AnchorMarker.shadow(ScreenAnchor.MIDDLE_RIGHT, AnchorPalette.GREEN)
        shadow.red() shouldBe 253
        shadow.green() shouldBe 64
        shadow.blue() shouldBe 160
        shadow.alpha() shouldBe 255
    }

    "as linhas descem 10px por linha a partir do ascent vanilla" {
        AnchoredLayout.ascents() shouldBe listOf(7, -3, -13, -23)
    }
})
