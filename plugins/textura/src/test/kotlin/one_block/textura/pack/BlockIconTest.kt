package one_block.textura.pack

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.awt.image.BufferedImage

class BlockIconTest : StringSpec({

    fun solid(argb: Int, size: Int = 16) = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB).apply {
        for (x in 0 until size) for (y in 0 until size) setRGB(x, y, argb)
    }

    fun alpha(argb: Int) = argb ushr 24

    "monta o cubo com topo claro, frente a esquerda e lado a direita mais escuros" {
        val icon = BlockIcon.render(BlockFaces(solid(0xFFFF0000.toInt()), solid(0xFF00FF00.toInt()), solid(0xFF0000FF.toInt())))

        icon.width shouldBe 16
        icon.height shouldBe 16
        icon.getRGB(8, 3) shouldBe 0xFFFF0000.toInt()
        icon.getRGB(3, 10) shouldBe 0xFF00CC00.toInt()
        icon.getRGB(12, 10) shouldBe 0xFF000099.toInt()
    }

    "em alta resolucao o cubo mantem as mesmas faces nas mesmas proporcoes" {
        val icon = BlockIcon.render(BlockFaces(solid(0xFFFF0000.toInt()), solid(0xFF00FF00.toInt()), solid(0xFF0000FF.toInt())), 48)

        icon.width shouldBe 48
        icon.getRGB(24, 9) shouldBe 0xFFFF0000.toInt()
        icon.getRGB(9, 30) shouldBe 0xFF00CC00.toInt()
        icon.getRGB(36, 30) shouldBe 0xFF000099.toInt()
        alpha(icon.getRGB(0, 0)) shouldBe 0
    }

    "o cubo tem o tamanho do inventario vanilla e deixa margem dos lados" {
        val icon = BlockIcon.render(BlockFaces(solid(-1), solid(-1), solid(-1)), 48)
        val opaque = (0 until 48).filter { x -> (0 until 48).any { y -> alpha(icon.getRGB(x, y)) != 0 } }

        opaque.first() shouldBe 3
        opaque.last() shouldBe 44
    }

    "os cantos fora do cubo ficam transparentes" {
        val icon = BlockIcon.render(BlockFaces(solid(-1), solid(-1), solid(-1)))

        alpha(icon.getRGB(0, 0)) shouldBe 0
        alpha(icon.getRGB(15, 0)) shouldBe 0
        alpha(icon.getRGB(0, 15)) shouldBe 0
        alpha(icon.getRGB(15, 15)) shouldBe 0
    }

    "topo vazado mostra o fundo por baixo" {
        val top = solid(0)
        val icon = BlockIcon.render(BlockFaces(top, solid(-1), solid(-1), under = solid(0xFF123456.toInt())))

        icon.getRGB(8, 4) shouldBe 0xFF123456.toInt()
    }

    "faces procuram topo, frente, lado e fundo com as variações do vanilla" {
        val present = setOf(
            "block/cartography_table_top", "block/cartography_table_side1", "block/cartography_table_side2",
            "block/composter_top", "block/composter_side", "block/composter_bottom",
            "block/oak_planks",
        )
        fun look(name: String) = VanillaTextures.faces(name) { it.takeIf(present::contains) }

        look("cartography_table") shouldBe BlockFaces("block/cartography_table_top", "block/cartography_table_side1", "block/cartography_table_side2")
        look("composter") shouldBe BlockFaces("block/composter_top", "block/composter_side", "block/composter_side", "block/composter_bottom")
        look("oak_planks") shouldBe BlockFaces("block/oak_planks", "block/oak_planks", "block/oak_planks")
        look("hopper").shouldBeNull()
    }
})
