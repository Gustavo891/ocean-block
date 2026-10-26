package one_block.textura.hud.anchor

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class AnchorSlideTest : StringSpec({

    "fora do centro a fase vai numa ancora de deslizar, id da ancora mais 8" {
        val color = AnchorMarker.animatedGlyph(ScreenAnchor.TOP_RIGHT, AnchorPalette.WHITE, 0x5A)
        color.red() shouldBe AnchorMarker.GLYPH_RED
        color.green() shouldBe (ScreenAnchor.TOP_RIGHT.id + 8) * AnchorMarker.STEP + 5
        color.blue() shouldBe AnchorPalette.WHITE.index * AnchorMarker.STEP + 10
    }

    "no centro a fase continua na ancora 8" {
        AnchorMarker.animatedGlyph(ScreenAnchor.CENTER, AnchorPalette.WHITE, 0x5A).green() shouldBe 8 * AnchorMarker.STEP + 5
    }

    "a sombra usa o mesmo codigo" {
        val shadow = AnchorMarker.animatedShadow(ScreenAnchor.TOP_LEFT, AnchorPalette.WHITE, 37)
        shadow.red() shouldBe AnchorMarker.SHADOW_RED
        shadow.green() shouldBe (ScreenAnchor.TOP_LEFT.id + 8) * AnchorMarker.STEP + 2
        shadow.blue() shouldBe AnchorPalette.WHITE.index * AnchorMarker.STEP + 5
    }
})
