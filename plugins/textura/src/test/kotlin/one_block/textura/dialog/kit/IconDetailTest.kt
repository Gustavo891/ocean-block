package one_block.textura.dialog.kit

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.awt.image.BufferedImage

class IconDetailTest : StringSpec({

    fun opaque(width: Int, height: Int, lastColumn: Int = width - 1) = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).apply {
        for (x in 0..lastColumn) for (y in 0 until height) setRGB(x, y, 0xFF808080.toInt())
    }

    "arte maior que o icone guarda ate 4x de detalhe" {
        IconScale.detail(opaque(128, 128), 32) shouldBe 4
        IconScale.detail(opaque(128, 128), 16) shouldBe 4
        IconScale.detail(opaque(64, 64), 32) shouldBe 2
        IconScale.detail(opaque(16, 16), 32) shouldBe 1
        IconScale.detail(opaque(9, 9), 9) shouldBe 1
    }

    "avanco do glyph com detalhe segue o arredondamento do cliente" {
        GlyphAdvance.of(opaque(32, 32)) shouldBe 33
        GlyphAdvance.of(opaque(128, 128), 4) shouldBe 33
        GlyphAdvance.of(opaque(128, 128, lastColumn = 125), 4) shouldBe 33
        GlyphAdvance.of(opaque(128, 128, lastColumn = 120), 4) shouldBe 31
    }
})
