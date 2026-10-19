package one_block.textura.hud.anchor

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import one_block.textura.registry.TitleFontRegistry
import one_block.textura.text.TextMetrics

data class AnchorSpan(val text: String, val palette: AnchorPalette)

data class AnchorLine(val anchor: ScreenAnchor, val row: Int, val spans: List<AnchorSpan>)

data class CenteredLine(val row: Int, val text: String, val color: TextColor)

object AnchoredLayout {

    const val ROW_HEIGHT = 10
    const val MAX_ROWS = 4

    fun ascent(row: Int): Int = TitleFontRegistry.BASE_ASCENT - row * ROW_HEIGHT

    fun ascents(): List<Int> = (0 until MAX_ROWS).map(::ascent)

    fun compose(lines: List<AnchorLine>, centered: List<CenteredLine>): Component =
        (lines.map(::anchored) + centered.map(::centered))
            .fold(Component.empty()) { acc, part -> acc.append(part) }

    fun anchored(line: AnchorLine): Component {
        val width = line.spans.sumOf { TextMetrics.calculateWidth(it.text) }
        val text = line.spans.fold(Component.empty().font(font(line.row))) { acc, span ->
            acc.append(
                Component.text(span.text)
                    .color(AnchorMarker.glyph(line.anchor, span.palette))
                    .shadowColor(AnchorMarker.shadow(line.anchor, span.palette)),
            )
        }
        val back = TextMetrics.getSpacingComponent(-width)
        return if (line.anchor.alignsRight) {
            Component.empty().append(back).append(text)
        } else {
            Component.empty().append(text).append(back)
        }
    }

    fun centered(line: CenteredLine): Component {
        val width = TextMetrics.calculateWidth(line.text)
        val left = width / 2
        return Component.empty()
            .append(TextMetrics.getSpacingComponent(-left))
            .append(Component.text(line.text).font(font(line.row)).color(line.color))
            .append(TextMetrics.getSpacingComponent(-(width - left)))
    }

    private fun font(row: Int): Key {
        require(row in 0 until MAX_ROWS) { "linha $row fora de 0..${MAX_ROWS - 1}" }
        return Key.key(Key.MINECRAFT_NAMESPACE, TitleFontRegistry.fontId(ascent(row), TitleFontRegistry.BASE_HEIGHT))
    }
}
