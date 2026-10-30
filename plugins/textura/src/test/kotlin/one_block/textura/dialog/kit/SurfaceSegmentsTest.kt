package one_block.textura.dialog.kit

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe

class SurfaceSegmentsTest : StringSpec({

    "surface sem detalhe ate 256 fica num segmento so" {
        SurfaceComposer.segments(220, 1, 256) shouldBe listOf(0 to 220)
    }

    "surface em 3x e cortada em segmentos de no maximo 256 px de textura" {
        val segments = SurfaceComposer.segments(176, 3, 256)

        segments shouldBe listOf(0 to 59, 59 to 59, 118 to 58)
        segments.forEach { (_, height) -> height * 3 shouldBeLessThanOrEqual 256 }
        segments.sumOf { it.second } shouldBe 176
    }

    "nenhum segmento fica baixo demais para o ascent da fonte" {
        (8..256).forEach { height ->
            SurfaceComposer.segments(height, 3, 256).forEach { (_, size) -> (size >= 8) shouldBe true }
        }
    }

    "pecas de surface em 3x nunca passam de 256 px de textura na largura" {
        val strip = java.awt.image.BufferedImage(48, 85 * 3, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        val pieces = UiPieces.slices("s", strip, 12, 12, 3)

        pieces.forEach {
            it.image.width shouldBeLessThanOrEqual UiPieces.MAX_TEXTURE
            it.image.height shouldBeLessThanOrEqual UiPieces.MAX_TEXTURE
        }
        pieces.map { it.name }.filter { it.contains("#mid") } shouldBe (0..6).map { "s#mid${1 shl it}" }
    }
})
