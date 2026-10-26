package ocean_block.system.islands.dialog

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.awt.image.BufferedImage

private const val GRAY = 0xFF808080.toInt()
private const val RED = 0xFFC83232.toInt()
private const val GREEN = 0xFF32C832.toInt()
private const val BLUE = 0xFF3232C8.toInt()
private const val YELLOW = 0xFFC8C832.toInt()
private const val CYAN = 0xFF32C8C8.toInt()

private fun canvas(width: Int = 64, height: Int = 64, build: BufferedImage.(fill: (Int, Int, Int, Int, Int) -> Unit) -> Unit = {}) =
    BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).apply {
        val fill = { x: Int, y: Int, w: Int, h: Int, argb: Int -> for (i in x until x + w) for (j in y until y + h) setRGB(i, j, argb) }
        fill(0, 0, width, height, GRAY)
        if (height == 64) {
            fill(32, 0, 32, 16, 0)
            fill(16, 32, 40, 16, 0)
            fill(0, 32, 16, 16, 0)
            fill(48, 48, 16, 16, 0)
            fill(0, 48, 16, 16, 0)
        }
        build(fill)
    }

private fun skin() = canvas { fill ->
    fill(8, 8, 8, 8, RED)
    fill(8, 0, 8, 8, GREEN)
    fill(16, 8, 8, 8, BLUE)
    fill(20, 20, 8, 12, YELLOW)
}

private fun Int.r() = (this shr 16) and 0xFF
private fun Int.g() = (this shr 8) and 0xFF
private fun Int.b() = this and 0xFF

private fun BufferedImage.pixels(): List<Int> = (0 until width).flatMap { x -> (0 until height).map { y -> getRGB(x, y) } }
    .filter { (it ushr 24) == 0xFF }

private fun BufferedImage.has(test: (Int) -> Boolean) = pixels().any(test)

private val redLike = { p: Int -> p.r() > 2 * p.g() && p.r() > 2 * p.b() }
private val greenLike = { p: Int -> p.g() > 2 * p.r() && p.g() > 2 * p.b() }
private val blueLike = { p: Int -> p.b() > 2 * p.r() && p.b() > 2 * p.g() }
private val yellowLike = { p: Int -> p.r() > 2 * p.b() && p.g() > 2 * p.b() && kotlin.math.abs(p.r() - p.g()) < 20 }
private val cyanLike = { p: Int -> p.g() > 2 * p.r() && p.b() > 2 * p.r() && kotlin.math.abs(p.g() - p.b()) < 20 }

class PlayerFigureTest : StringSpec({

    "o corpo inteiro cabe na imagem com fundo transparente" {
        val image = PlayerFigure.render(skin(), slim = false)

        image.width shouldBe PlayerFigure.WIDTH
        image.height shouldBe PlayerFigure.HEIGHT
        (image.getRGB(0, 0) ushr 24) shouldBe 0
        (image.getRGB(image.width - 1, image.height - 1) ushr 24) shouldBe 0
    }

    "mostra a frente, o topo e o lado esquerdo da cabeça e o peito" {
        val image = PlayerFigure.render(skin(), slim = false)

        image.has(redLike) shouldBe true
        image.has(greenLike) shouldBe true
        image.has(blueLike) shouldBe true
        image.has(yellowLike) shouldBe true
    }

    "o chapéu opaco cobre a frente da cabeça e o transparente deixa ver" {
        val hat = canvas { fill ->
            fill(8, 8, 8, 8, RED)
            fill(40, 8, 8, 8, CYAN)
        }

        PlayerFigure.render(hat, slim = false).has(cyanLike) shouldBe true
        PlayerFigure.render(hat, slim = false).has(redLike) shouldBe false
        PlayerFigure.render(skin(), slim = false).has(cyanLike) shouldBe false
    }

    "braço fino ocupa menos pixels que o largo" {
        PlayerFigure.render(skin(), slim = true).pixels().size shouldBeLessThanSize PlayerFigure.render(skin(), slim = false).pixels().size
    }

    "virado de costas mostra a nuca e esconde o rosto" {
        val back = canvas { fill ->
            fill(8, 8, 8, 8, RED)
            fill(24, 8, 8, 8, CYAN)
        }

        val image = PlayerFigure.render(back, slim = false, yaw = 180.0)

        image.has(cyanLike) shouldBe true
        image.has(redLike) shouldBe false
    }

    "o busto mostra rosto, topo e o lado direito da cabeça no tamanho do palco" {
        val bust = canvas { fill ->
            fill(8, 8, 8, 8, RED)
            fill(8, 0, 8, 8, GREEN)
            fill(0, 8, 8, 8, BLUE)
        }

        val image = PlayerFigure.bust(bust, slim = false)

        image.width shouldBe PlayerFigure.BUST_WIDTH
        image.height shouldBe PlayerFigure.BUST_HEIGHT
        image.has(redLike) shouldBe true
        image.has(greenLike) shouldBe true
        image.has(blueLike) shouldBe true
    }

    "skin antiga de 64x32 também desenha" {
        PlayerFigure.render(canvas(height = 32), slim = false).pixels().isNotEmpty() shouldBe true
    }
})

private infix fun Int.shouldBeLessThanSize(other: Int) = (this < other) shouldBe true
