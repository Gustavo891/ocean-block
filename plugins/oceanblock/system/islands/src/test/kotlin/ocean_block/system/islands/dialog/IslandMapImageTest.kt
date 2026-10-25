package ocean_block.system.islands.dialog

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private const val GRASS = 0x7FB238
private const val WATER = 0x4040FF

class IslandMapImageTest : StringSpec({

    fun flat(size: Int, column: MapColumn) = List(size) { List(size) { column } }

    "terreno plano usa o tom base da cor do mapa" {
        val image = IslandMapImage.render(flat(4, MapColumn(GRASS, 64, 0)), marker = null)

        image.width shouldBe 4
        image.height shouldBe 4
        image.getRGB(1, 1) shouldBe IslandMapImage.shade(GRASS, IslandMapImage.BASE)
    }

    "coluna mais alta que a do norte fica mais clara e mais baixa fica mais escura" {
        val grid = listOf(
            listOf(MapColumn(GRASS, 64, 0), MapColumn(GRASS, 64, 0)),
            listOf(MapColumn(GRASS, 66, 0), MapColumn(GRASS, 62, 0)),
        )

        val image = IslandMapImage.render(grid, marker = null)

        image.getRGB(0, 1) shouldBe IslandMapImage.shade(GRASS, IslandMapImage.LIGHT)
        image.getRGB(1, 1) shouldBe IslandMapImage.shade(GRASS, IslandMapImage.DARK)
    }

    "água escurece com a profundidade" {
        val grid = listOf(listOf(MapColumn(WATER, 62, 1), MapColumn(WATER, 62, 4), MapColumn(WATER, 62, 20)))

        val image = IslandMapImage.render(grid, marker = null)

        image.getRGB(0, 0) shouldBe IslandMapImage.shade(WATER, IslandMapImage.LIGHT)
        image.getRGB(1, 0) shouldBe IslandMapImage.shade(WATER, IslandMapImage.BASE)
        image.getRGB(2, 0) shouldBe IslandMapImage.shade(WATER, IslandMapImage.DARK)
    }

    "o marcador do jogador fica branco com borda escura" {
        val image = IslandMapImage.render(flat(9, MapColumn(GRASS, 64, 0)), marker = 4 to 4)

        image.getRGB(4, 4) shouldBe IslandMapImage.MARKER
        image.getRGB(4, 2) shouldBe IslandMapImage.MARKER_EDGE
        image.getRGB(0, 0) shouldBe IslandMapImage.shade(GRASS, IslandMapImage.BASE)
    }

    "marcador na borda não estoura a imagem" {
        val image = IslandMapImage.render(flat(3, MapColumn(GRASS, 64, 0)), marker = 0 to 0)

        image.getRGB(0, 0) shouldBe IslandMapImage.MARKER
    }
})
