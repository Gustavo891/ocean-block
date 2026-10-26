package one_block.textura.notify.render

import one_block.textura.notify.NotificationLine

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.TextColor
import one_block.textura.api.TexturaTags
import one_block.textura.dialog.kit.UiTextFont
import one_block.textura.hud.anchor.AnchorMarker
import one_block.textura.hud.anchor.AnchorPalette
import one_block.textura.hud.anchor.ScreenAnchor
import one_block.textura.notify.layout.LineGeometry
import one_block.textura.notify.region.NotificationRegion
import one_block.textura.notify.region.StackDirection
import one_block.textura.notify.render.IconGlyph
import one_block.textura.notify.render.NotificationFonts
import one_block.textura.notify.render.NotificationKit
import one_block.textura.notify.render.NotificationRenderer
import one_block.textura.notify.render.PlaqueGlyphs
import one_block.textura.notify.render.TextPainter
import one_block.textura.notify.render.TranslationFormat
import one_block.textura.text.SpacingFont

class NotificationRendererTest : StringSpec({

    val textFont = UiTextFont(emptyList(), mapOf(' '.code to 4, 'i'.code to 2))
    val bodies = listOf(128, 64, 32, 16, 8, 4, 2, 1).associateWith { "B$it" }
    val kit = NotificationKit(
        textFont,
        setOf(0, 4, 70),
        PlaqueGlyphs("L", "R", bodies),
        mapOf("item:oak_planks" to IconGlyph("I", 9)),
    )
    val glyphAdvance = mapOf("L" to 2, "R" to 2, "I" to 9) + bodies.map { (width, char) -> char to width + 1 }
    val renderer = NotificationRenderer(TexturaTags.miniMessage)
    val translations = mapOf("item.minecraft.oak_planks" to "Tábuas")

    data class Draw(val text: String, val x: Int, val color: TextColor?)

    fun draws(component: Component): Pair<List<Draw>, Int> {
        val out = mutableListOf<Draw>()
        var x = 0
        fun walk(c: Component, color: TextColor?, font: Key?) {
            val effectiveColor = c.color() ?: color
            val effectiveFont = c.font() ?: font
            if (c is TextComponent && c.content().isNotEmpty()) {
                val content = c.content()
                if (effectiveFont == SpacingFont.KEY) {
                    x += content.sumOf { SpacingFont.ADVANCES.getValue(it.toString()) }
                } else {
                    out += Draw(content, x, effectiveColor)
                    x += glyphAdvance[content] ?: content.codePoints().toArray().sumOf { textFont.advance(it) }
                }
            }
            c.children().forEach { walk(it, effectiveColor, effectiveFont) }
        }
        walk(component, null, null)
        return out to x
    }

    fun region(anchor: ScreenAnchor, x: Int, y: Int, direction: StackDirection, background: Boolean) =
        NotificationRegion("r", anchor, x, y, 6, 1, background, direction)

    fun render(region: NotificationRegion, vararg lines: NotificationLine): Component =
        renderer.render(listOf(region), mapOf(region.id to lines.toList()), kit) { translations[it] }

    fun plaqueOnly(items: List<Draw>) = items.filter { it.text == "L" || it.text == "R" || it.text.startsWith("B") }

    "cada linha devolve o cursor à origem" {
        val right = region(ScreenAnchor.MIDDLE_RIGHT, 0, 70, StackDirection.DOWN, true)
        val (_, end) = draws(render(right, NotificationLine("<green>+3</green> Tábua", "item:oak_planks", 60), NotificationLine("oi", 60)))
        end shouldBe 0
    }

    "à direita o fundo termina no x e o texto termina no padding" {
        val right = region(ScreenAnchor.MIDDLE_RIGHT, -1, 70, StackDirection.DOWN, true)
        val (items, _) = draws(render(right, NotificationLine("abc", 60)))
        val textWidth = 18
        val box = textWidth + 2 * LineGeometry.PADDING_X
        val plaque = plaqueOnly(items)
        plaque.first().text shouldBe "L"
        plaque.first().x shouldBe -1 - box
        plaque.last().text shouldBe "R"
        plaque.last().x shouldBe -1 - 1
        items.single { it.text == "abc" }.x shouldBe -1 - LineGeometry.PADDING_X - textWidth
    }

    "fundo cobre exatamente a largura do conteúdo com ícone" {
        val left = region(ScreenAnchor.TOP_LEFT, 4, 4, StackDirection.DOWN, true)
        val (items, _) = draws(render(left, NotificationLine("ab", "item:oak_planks", 60)))
        plaqueOnly(items).sumOf { glyphAdvance.getValue(it.text) - 1 } shouldBe 12 + LineGeometry.ICON_BOX + 2 * LineGeometry.PADDING_X
        items.single { it.text == "I" }.x shouldBe 4 + LineGeometry.PADDING_X
        items.single { it.text == "ab" }.x shouldBe 4 + LineGeometry.PADDING_X + LineGeometry.ICON_BOX
    }

    "peças da placa ficam lado a lado sem sobrepor" {
        val left = region(ScreenAnchor.TOP_LEFT, 0, 0, StackDirection.DOWN, true)
        val plaque = plaqueOnly(draws(render(left, NotificationLine("abcdefg", 60))).first)
        plaque.zipWithNext().forEach { (a, b) -> b.x shouldBe a.x + glyphAdvance.getValue(a.text) - 1 }
    }

    "sem fundo não há placa e o texto encosta no x" {
        val left = region(ScreenAnchor.TOP_LEFT, 4, 4, StackDirection.DOWN, false)
        val (items, end) = draws(render(left, NotificationLine("ab", 60)))
        items.map { it.text }.shouldContainExactly("ab")
        items.single().x shouldBe 4
        end shouldBe 0
    }

    "cores levam âncora, paleta e lift; sombra explícita R 253" {
        val base = region(ScreenAnchor.BOTTOM_LEFT, 0, 0, StackDirection.UP, false)
        val component = render(base, NotificationLine("<green>a</green>", 60), NotificationLine("<red>b</red>", 60))
        val (items, _) = draws(component)
        items.single { it.text == "a" }.color shouldBe AnchorMarker.glyph(ScreenAnchor.BOTTOM_LEFT, AnchorPalette.GREEN, 0)
        items.single { it.text == "b" }.color shouldBe AnchorMarker.glyph(ScreenAnchor.BOTTOM_LEFT, AnchorPalette.RED, 2)

        fun shadows(c: Component): List<Int> = listOfNotNull(c.shadowColor()?.value()) + c.children().flatMap(::shadows)
        shadows(component).filter { it ushr 24 != 0 }.map { (it shr 16) and 0xFF }.toSet() shouldBe setOf(AnchorMarker.SHADOW_RED)
    }

    "placa e ícone usam branco da paleta e não têm sombra" {
        val left = region(ScreenAnchor.TOP_LEFT, 0, 0, StackDirection.DOWN, true)
        val component = render(left, NotificationLine("a", "item:oak_planks", 60))
        val (items, _) = draws(component)
        items.filter { it.text != "a" }.map { it.color }.toSet() shouldBe setOf(AnchorMarker.glyph(ScreenAnchor.TOP_LEFT, AnchorPalette.WHITE, 0))
    }

    "linhas usam a fonte da sua altura" {
        val left = region(ScreenAnchor.TOP_LEFT, 4, 4, StackDirection.DOWN, false)
        fun fonts(c: Component): List<Key> = listOfNotNull(c.font()) + c.children().flatMap(::fonts)
        fonts(render(left, NotificationLine("a", 60), NotificationLine("b", 60))).filter { it != SpacingFont.KEY }.toSet() shouldBe
            setOf(NotificationFonts.key(4), NotificationFonts.key(18))
    }

    "regiões aparecem na ordem da configuração e respeitam o limite" {
        val first = NotificationRegion("a", ScreenAnchor.TOP_LEFT, 0, 0, 1, 1, false, StackDirection.DOWN)
        val second = NotificationRegion("b", ScreenAnchor.TOP_RIGHT, 0, 0, 2, 1, false, StackDirection.DOWN)
        val component = renderer.render(
            listOf(first, second),
            mapOf("b" to listOf(NotificationLine("y", 60)), "a" to listOf(NotificationLine("x", 60), NotificationLine("z", 60))),
            kit,
        ) { null }
        draws(component).first.map { it.text }.shouldContainExactly("x", "y")
    }

    "lang é resolvido para texto e medido com a tradução" {
        val painter = TextPainter(textFont) { translations[it] }
        val painted = painter.paint(TexturaTags.miniMessage.deserialize("<lang:item.minecraft.oak_planks>"), ScreenAnchor.TOP_LEFT, 0, NotificationFonts.key(0))
        painted.width shouldBe 36
        fun translatables(c: Component): Int = (if (c is TranslatableComponent) 1 else 0) + c.children().sumOf(::translatables)
        translatables(painted.component) shouldBe 0
    }

    "negrito soma 1px por caractere, espaço incluído, e shift conta o avanço real" {
        val painter = TextPainter(textFont) { null }
        painter.paint(TexturaTags.miniMessage.deserialize("<b>ab c</b>"), ScreenAnchor.TOP_LEFT, 0, NotificationFonts.key(0)).width shouldBe 6 * 3 + 4 + 4
        painter.paint(TexturaTags.miniMessage.deserialize("a<shift:-3>b"), ScreenAnchor.TOP_LEFT, 0, NotificationFonts.key(0)).width shouldBe 9
    }

    "formato de tradução respeita argumentos posicionais" {
        TranslationFormat.split("%2\$s de %1\$s %%", listOf(Component.text("A"), Component.text("B")))
            .map { (it as TextComponent).content() }.shouldContainExactly("B", " de ", "A", " ", "%")
    }

    "ícone desconhecido some sem quebrar a linha" {
        val left = region(ScreenAnchor.TOP_LEFT, 0, 0, StackDirection.DOWN, false)
        val (items, end) = draws(render(left, NotificationLine("a", "item:diamond", 60)))
        items.map { it.text }.shouldContainExactly("a")
        end shouldBe 0
    }

    "sem kit montado a linha sai só com texto" {
        val left = region(ScreenAnchor.TOP_LEFT, 0, 0, StackDirection.DOWN, true)
        val component = renderer.render(listOf(left), mapOf("r" to listOf(NotificationLine("a", "item:oak_planks", 60))), NotificationKit.textOnly(textFont)) { null }
        val (items, end) = draws(component)
        items.map { it.text }.shouldContainExactly("a")
        end shouldBe 0
    }

    "regiao animada leva a fase nos bits livres do marcador da ancora central" {
        val center = NotificationRegion("xp", ScreenAnchor.CENTER, 8, 0, 1, 1, false, StackDirection.DOWN, animated = true)
        val component = renderer.render(
            listOf(center),
            mapOf("xp" to listOf(NotificationLine("<green>+8</green> XP", 40))),
            kit,
            mapOf("xp" to listOf(37)),
        ) { null }

        val (items, end) = draws(component)
        end shouldBe 0
        items.first().x shouldBe 8
        items.forEach { draw ->
            draw.color?.green() shouldBe ScreenAnchor.CENTER.id * AnchorMarker.STEP + 2
            draw.color?.blue()?.rem(AnchorMarker.STEP) shouldBe 5
        }
    }

    fun toastLine(backdrop: BackdropGlyph, advance: Int): Pair<List<Pair<String, Int>>, Int> {
        val toastKit = NotificationKit(textFont, setOf(0), null, emptyMap(), mapOf("glyph:toast_x" to backdrop))
        val corner = NotificationRegion("toasts", ScreenAnchor.TOP_RIGHT, 0, 0, 6, 1, false, StackDirection.DOWN)
        val component = renderer.render(listOf(corner), mapOf("toasts" to listOf(NotificationLine("ab", "glyph:toast_x", 100))), toastKit) { null }

        val out = mutableListOf<Pair<String, Int>>()
        var x = 0
        fun walk(c: Component) {
            if (c is TextComponent && c.content().isNotEmpty()) {
                if (c.font() == SpacingFont.KEY) x += c.content().sumOf { SpacingFont.ADVANCES.getValue(it.toString()) }
                else { out += c.content() to x; x += if (c.content() == backdrop.char) advance else c.content().codePoints().toArray().sumOf { textFont.advance(it) } }
            }
            c.children().forEach(::walk)
        }
        walk(component)
        return out to x
    }

    "linha com fundo de toast desenha o fundo na largura dele e o texto 36 px para dentro" {
        val (out, x) = toastLine(BackdropGlyph("T", 132, 133), 133)

        out.first() shouldBe ("T" to -132)
        out.last().second shouldBe -132 + 36
        x shouldBe 0
    }

    "fundo de toast transparente avança só o que o cliente mede e o texto fica no mesmo recuo" {
        val (out, x) = toastLine(BackdropGlyph("L", 132, 1), 1)

        out.first() shouldBe ("L" to -132)
        out.last().second shouldBe -132 + 36
        x shouldBe 0
    }
})
