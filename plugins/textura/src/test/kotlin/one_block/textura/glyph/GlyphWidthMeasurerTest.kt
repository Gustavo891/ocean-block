package one_block.textura.glyph

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.util.logging.Logger
import javax.imageio.ImageIO

class GlyphWidthMeasurerTest : StringSpec({

    val logger = Logger.getLogger("GlyphWidthMeasurerTest")

    fun tempDir(): File = Files.createTempDirectory("textura-test").toFile().apply { deleteOnExit() }

    fun png(dir: File, name: String, width: Int, height: Int): File {
        val file = File(dir, name)
        ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", file)
        return file
    }

    "mede a largura da imagem" {
        GlyphWidthMeasurer.measure(png(tempDir(), "folha.png", 16, 48), logger) shouldBe 16
    }

    "arquivo inexistente devolve null" {
        GlyphWidthMeasurer.measure(File(tempDir(), "nao_existe.png"), logger) shouldBe null
    }

    "arquivo que nao e imagem devolve null" {
        val file = File(tempDir(), "quebrado.png")
        file.writeText("nao sou um png")
        GlyphWidthMeasurer.measure(file, logger) shouldBe null
    }
})
