package one_block.textura.notify.render

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.minimessage.MiniMessage
import one_block.textura.hud.anchor.AnchorMarker
import one_block.textura.hud.anchor.AnchorPalette
import one_block.textura.notify.NotificationLine
import one_block.textura.notify.layout.LineGeometry
import one_block.textura.notify.layout.PlaquePieces
import one_block.textura.notify.region.NotificationRegion
import one_block.textura.text.SpacingFont

internal class NotificationRenderer(private val miniMessage: MiniMessage) {

    private class Cursor {
        val parts = mutableListOf<Component>()
        var x = 0
            private set

        fun moveTo(target: Int) {
            if (target == x) return
            parts += SpacingFont.component(target - x)
            x = target
        }

        fun draw(component: Component, advance: Int) {
            parts += component
            x += advance
        }

        fun build(): Component = Component.empty().children(parts)
    }

    fun render(regions: List<NotificationRegion>, lines: Map<String, List<NotificationLine>>, kit: NotificationKit, translate: (String) -> String?): Component =
        render(regions, lines, kit, emptyMap(), translate)

    fun render(
        regions: List<NotificationRegion>,
        lines: Map<String, List<NotificationLine>>,
        kit: NotificationKit,
        phases: Map<String, List<Int>>,
        translate: (String) -> String?,
    ): Component {
        val painter = TextPainter(kit.textFont, translate)
        val parts = regions.flatMap { region ->
            lines[region.id].orEmpty().take(region.maxLines).mapIndexedNotNull { index, line ->
                line(region, index, line, kit, painter, phases[region.id]?.getOrNull(index) ?: 0)
            }
        }
        return Component.empty().children(parts)
    }

    fun line(region: NotificationRegion, index: Int, line: NotificationLine, kit: NotificationKit, painter: TextPainter, phase: Int): Component? {
        val slot = LineGeometry.slot(region, index) ?: return null
        val animatedPhase = phase.takeIf { region.animated }
        val font = NotificationFonts.key(slot.textTop)
        val backdrop = kit.backdrop(line.icon)
        val textFont = backdrop?.let { raisedFont(slot.textTop, it.raise, kit) } ?: font
        val text = painter.paint(miniMessage.deserialize(line.text), region.anchor, slot.lift, textFont, animatedPhase)
        val plain = animatedPhase?.let { AnchorMarker.animatedGlyph(region.anchor, AnchorPalette.WHITE, it) }
            ?: AnchorMarker.glyph(region.anchor, AnchorPalette.WHITE, slot.lift)
        if (backdrop != null) {
            val box = LineGeometry.span(region, backdrop.width).boxLeft
            val cursor = Cursor()
            cursor.moveTo(box)
            cursor.draw(glyph(backdrop.char, font, plain), backdrop.advance)
            cursor.moveTo(box + LineGeometry.BACKDROP_TEXT_X)
            cursor.draw(Component.text().font(textFont).append(text.component).build(), text.width)
            cursor.moveTo(0)
            return cursor.build()
        }
        val icon = kit.icon(line.icon)
        val span = LineGeometry.span(region, LineGeometry.contentWidth(text.width, icon != null))
        val cursor = Cursor()

        val plaque = kit.plaque
        if (region.background && plaque != null && span.boxWidth >= 2 * PlaquePieces.CAP_WIDTH) {
            cursor.moveTo(span.boxLeft)
            fun piece(char: String, width: Int) {
                cursor.draw(glyph(char, font, plain), width + 1)
                cursor.moveTo(cursor.x - 1)
            }
            piece(plaque.leftCap, PlaquePieces.CAP_WIDTH)
            PlaquePieces.body(span.boxWidth).forEach { width -> plaque.bodies[width]?.let { piece(it, width) } }
            piece(plaque.rightCap, PlaquePieces.CAP_WIDTH)
        }

        cursor.moveTo(span.contentLeft)
        if (icon != null) {
            cursor.draw(glyph(icon.char, font, plain), icon.advance)
            cursor.moveTo(span.contentLeft + LineGeometry.ICON_BOX)
        }
        cursor.draw(Component.text().font(font).append(text.component).build(), text.width)
        cursor.moveTo(0)
        return cursor.build()
    }

    private fun raisedFont(top: Int, raise: Int, kit: NotificationKit): Key? =
        (top - raise).takeIf { raise > 0 && it in kit.textTops }?.let(NotificationFonts::key)

    private fun glyph(char: String, font: Key, color: net.kyori.adventure.text.format.TextColor): Component =
        Component.text(char).font(font).color(color).shadowColor(ShadowColor.none())
}
