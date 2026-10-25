package one_block.textura.hud

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor
import one_block.textura.hud.anchor.AnchorMarker
import one_block.textura.hud.anchor.AnchorPalette

object HudRaise {

    private const val OPAQUE = 255

    fun mark(component: Component): Component = visit(component, null, null)

    private fun visit(component: Component, parentColor: TextColor?, parentShadow: ShadowColor?): Component {
        val color = component.style().color() ?: parentColor
        val shadow = component.style().shadowColor() ?: parentShadow
        val palette = AnchorPalette.nearest(color)
        val markedShadow = if (shadow != null && shadow.alpha() == 0) ShadowColor.none() else AnchorMarker.raisedShadow(palette, OPAQUE)
        return component
            .color(AnchorMarker.raisedGlyph(palette))
            .shadowColor(markedShadow)
            .children(component.children().map { visit(it, color, shadow) })
    }
}
