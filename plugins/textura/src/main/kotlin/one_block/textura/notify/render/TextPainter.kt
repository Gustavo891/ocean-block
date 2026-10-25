package one_block.textura.notify.render

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.TranslationArgument
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import one_block.textura.dialog.kit.UiTextFont
import one_block.textura.hud.anchor.AnchorMarker
import one_block.textura.hud.anchor.AnchorPalette
import one_block.textura.hud.anchor.ScreenAnchor
import one_block.textura.text.SpacingFont
import one_block.textura.text.TextMetrics

internal data class PaintedText(val component: Component, val width: Int)

internal class TextPainter(private val textFont: UiTextFont, private val translate: (String) -> String?) {

    private data class Context(
        val color: TextColor?,
        val bold: Boolean,
        val font: Key?,
        val shadow: ShadowColor?,
    )

    fun paint(component: Component, anchor: ScreenAnchor, lift: Int, font: Key): PaintedText = paint(component, anchor, lift, font, null)

    fun paint(component: Component, anchor: ScreenAnchor, lift: Int, font: Key, phase: Int?): PaintedText {
        val root = Context(null, false, font, null)
        return visit(component, root, Marker(anchor, lift, font, phase))
    }

    private data class Marker(val anchor: ScreenAnchor, val lift: Int, val font: Key, val phase: Int?) {

        fun glyph(palette: AnchorPalette) =
            if (phase != null) AnchorMarker.animatedGlyph(palette, phase) else AnchorMarker.glyph(anchor, palette, lift)

        fun shadow(palette: AnchorPalette) =
            if (phase != null) AnchorMarker.animatedShadow(palette, phase) else AnchorMarker.shadow(anchor, palette, lift)
    }

    private fun visit(component: Component, parent: Context, marker: Marker): PaintedText {
        val style = component.style()
        val context = Context(
            color = style.color() ?: parent.color,
            bold = when (style.decoration(TextDecoration.BOLD)) {
                TextDecoration.State.TRUE -> true
                TextDecoration.State.FALSE -> false
                else -> parent.bold
            },
            font = style.font() ?: parent.font,
            shadow = style.shadowColor() ?: parent.shadow,
        )

        val flat = flatten(component)
        val own = (flat as? TextComponent)?.let { measure(it.content(), context, marker.font) }
            ?: (flat as? TranslatableComponent)?.let { measure(it.fallback() ?: it.key(), context, marker.font) }
            ?: 0

        val children = flat.children().map { visit(it, context, marker) }
        val palette = AnchorPalette.nearest(context.color)
        val shadow = if (context.shadow != null && context.shadow.alpha() == 0) {
            ShadowColor.none()
        } else {
            marker.shadow(palette)
        }

        val painted = flat
            .color(marker.glyph(palette))
            .shadowColor(shadow)
            .children(children.map { it.component })
        return PaintedText(painted, own + children.sumOf { it.width })
    }

    private fun flatten(component: Component): Component {
        if (component !is TranslatableComponent) return component
        val pattern = translate(component.key()) ?: return component
        val parts = TranslationFormat.split(pattern, component.arguments().map(TranslationArgument::asComponent))
        return Component.text("").style(component.style()).children(parts + component.children())
    }

    private fun measure(text: String, context: Context, own: Key): Int = when {
        context.font == SpacingFont.KEY -> text.sumOf { (SpacingFont.ADVANCES[it.toString()] ?: 0) + if (context.bold) 1 else 0 }
        context.font != null && context.font != own -> TextMetrics.calculateWidth(text)
        else -> text.codePoints().toArray().sumOf { codePoint ->
            textFont.advance(codePoint) + if (context.bold) 1 else 0
        }
    }
}

internal object TranslationFormat {

    private val PLACEHOLDER = Regex("""%(?:(\d+)\$)?([sd%])""")

    fun split(pattern: String, arguments: List<Component>): List<Component> {
        val parts = mutableListOf<Component>()
        var next = 0
        var last = 0
        for (match in PLACEHOLDER.findAll(pattern)) {
            if (match.range.first > last) parts += Component.text(pattern.substring(last, match.range.first))
            last = match.range.last + 1
            if (match.groupValues[2] == "%") {
                parts += Component.text("%")
                continue
            }
            val index = match.groupValues[1].toIntOrNull()?.minus(1) ?: next++
            arguments.getOrNull(index)?.let { parts += it }
        }
        if (last < pattern.length) parts += Component.text(pattern.substring(last))
        return parts
    }
}
