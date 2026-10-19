package one_block.textura.hud.anchor

import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor

object AnchorMarker {

    const val GLYPH_RED = 254
    const val SHADOW_RED = 253
    const val STEP = 16
    const val MAX_LIFT = STEP - 1
    private const val OPAQUE = 255

    fun glyph(anchor: ScreenAnchor, palette: AnchorPalette): TextColor = glyph(anchor, palette, 0)

    fun shadow(anchor: ScreenAnchor, palette: AnchorPalette): ShadowColor = shadow(anchor, palette, 0)

    fun glyph(anchor: ScreenAnchor, palette: AnchorPalette, lift: Int): TextColor =
        TextColor.color(GLYPH_RED, green(anchor), blue(palette, lift))

    fun shadow(anchor: ScreenAnchor, palette: AnchorPalette, lift: Int): ShadowColor =
        ShadowColor.shadowColor(SHADOW_RED, green(anchor), blue(palette, lift), OPAQUE)

    private fun green(anchor: ScreenAnchor): Int = anchor.id * STEP

    private fun blue(palette: AnchorPalette, lift: Int): Int {
        require(lift in 0..MAX_LIFT) { "lift $lift fora de 0..$MAX_LIFT" }
        return palette.index * STEP + lift
    }
}
