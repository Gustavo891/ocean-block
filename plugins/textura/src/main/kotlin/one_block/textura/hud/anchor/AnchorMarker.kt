package one_block.textura.hud.anchor

import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor

object AnchorMarker {

    const val GLYPH_RED = 254
    const val SHADOW_RED = 253
    const val STEP = 16
    private const val OPAQUE = 255

    fun glyph(anchor: ScreenAnchor, palette: AnchorPalette): TextColor =
        TextColor.color(GLYPH_RED, anchor.id * STEP, palette.index * STEP)

    fun shadow(anchor: ScreenAnchor, palette: AnchorPalette): ShadowColor =
        ShadowColor.shadowColor(SHADOW_RED, anchor.id * STEP, palette.index * STEP, OPAQUE)
}
