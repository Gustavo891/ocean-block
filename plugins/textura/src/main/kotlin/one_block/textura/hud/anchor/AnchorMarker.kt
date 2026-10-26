package one_block.textura.hud.anchor

import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor

object AnchorMarker {

    const val GLYPH_RED = 254
    const val SHADOW_RED = 253
    const val STEP = 16
    const val MAX_LIFT = STEP - 1
    const val RAISE_ID = 7
    const val SLIDE_OFFSET = 8
    private const val OPAQUE = 255

    fun glyph(anchor: ScreenAnchor, palette: AnchorPalette): TextColor = glyph(anchor, palette, 0)

    fun shadow(anchor: ScreenAnchor, palette: AnchorPalette): ShadowColor = shadow(anchor, palette, 0)

    fun glyph(anchor: ScreenAnchor, palette: AnchorPalette, lift: Int): TextColor =
        TextColor.color(GLYPH_RED, green(anchor), blue(palette, lift))

    fun shadow(anchor: ScreenAnchor, palette: AnchorPalette, lift: Int): ShadowColor =
        ShadowColor.shadowColor(SHADOW_RED, green(anchor), blue(palette, lift), OPAQUE)

    fun raisedGlyph(palette: AnchorPalette): TextColor =
        TextColor.color(GLYPH_RED, RAISE_ID * STEP, blue(palette, 0))

    fun raisedShadow(palette: AnchorPalette, alpha: Int): ShadowColor =
        ShadowColor.shadowColor(SHADOW_RED, RAISE_ID * STEP, blue(palette, 0), alpha)

    fun animatedGlyph(anchor: ScreenAnchor, palette: AnchorPalette, phase: Int): TextColor =
        TextColor.color(GLYPH_RED, animatedGreen(anchor, phase), blue(palette, phase and MAX_LIFT))

    fun animatedShadow(anchor: ScreenAnchor, palette: AnchorPalette, phase: Int): ShadowColor =
        ShadowColor.shadowColor(SHADOW_RED, animatedGreen(anchor, phase), blue(palette, phase and MAX_LIFT), OPAQUE)

    private fun animatedGreen(anchor: ScreenAnchor, phase: Int): Int {
        require(phase in 0 until STEP * STEP) { "fase $phase fora de 0..${STEP * STEP - 1}" }
        val id = if (anchor == ScreenAnchor.CENTER) anchor.id else anchor.id + SLIDE_OFFSET
        return id * STEP + (phase shr 4)
    }

    private fun green(anchor: ScreenAnchor): Int = anchor.id * STEP

    private fun blue(palette: AnchorPalette, lift: Int): Int {
        require(lift in 0..MAX_LIFT) { "lift $lift fora de 0..$MAX_LIFT" }
        return palette.index * STEP + lift
    }
}
