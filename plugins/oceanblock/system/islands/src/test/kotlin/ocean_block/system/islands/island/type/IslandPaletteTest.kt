package ocean_block.system.islands.island.type

import io.kotest.core.spec.style.StringSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe

class IslandPaletteTest : StringSpec({
    "tabela do deserto" {
        forAll(
            row("minecraft:grass_block[snowy=false]", "minecraft:sand"),
            row("minecraft:dirt", "minecraft:sandstone"),
            row("minecraft:moss_block", "minecraft:smooth_sandstone"),
            row("minecraft:sand", "minecraft:sand"),
            row("minecraft:gravel", "minecraft:gravel"),
            row("minecraft:cobblestone", "minecraft:sandstone"),
            row("minecraft:mossy_cobblestone", "minecraft:cut_sandstone"),
            row("minecraft:short_grass", "minecraft:dead_bush"),
            row("minecraft:dandelion", "minecraft:dead_bush"),
            row("minecraft:poppy", "minecraft:dead_bush"),
            row("minecraft:tall_grass[half=lower]", "minecraft:dead_bush"),
            row("minecraft:tall_grass[half=upper]", "minecraft:air"),
            row("minecraft:oak_leaves[distance=1,persistent=true,waterlogged=false]", "minecraft:air"),
        ) { from, to -> IslandPalette.swap(IslandType.DESERTO, from) shouldBe to }
    }

    "tabela do gelo" {
        forAll(
            row("minecraft:grass_block[snowy=false]", "minecraft:snow_block"),
            row("minecraft:dirt", "minecraft:dirt"),
            row("minecraft:moss_block", "minecraft:snow_block"),
            row("minecraft:sand", "minecraft:snow_block"),
            row("minecraft:gravel", "minecraft:gravel"),
            row("minecraft:cobblestone", "minecraft:cobblestone"),
            row("minecraft:mossy_cobblestone", "minecraft:cobblestone"),
            row("minecraft:short_grass", "minecraft:snow[layers=1]"),
            row("minecraft:dandelion", "minecraft:snow[layers=1]"),
            row("minecraft:poppy", "minecraft:snow[layers=1]"),
            row("minecraft:tall_grass[half=lower]", "minecraft:snow[layers=2]"),
            row("minecraft:tall_grass[half=upper]", "minecraft:air"),
            row("minecraft:oak_log[axis=y]", "minecraft:spruce_log[axis=y]"),
        ) { from, to -> IslandPalette.swap(IslandType.GELO, from) shouldBe to }
    }

    "tronco do deserto vira arvore morta mantendo o eixo" {
        IslandPalette.swap(IslandType.DESERTO, "minecraft:oak_log[axis=x]") shouldBe "minecraft:stripped_oak_log[axis=x]"
    }

    "folha do gelo vira abeto mantendo o estado" {
        IslandPalette.swap(IslandType.GELO, "minecraft:oak_leaves[distance=3,persistent=true,waterlogged=false]") shouldBe
            "minecraft:spruce_leaves[distance=3,persistent=true,waterlogged=false]"
    }

    "escada mantem as quatro propriedades" {
        val stairs = "minecraft:mossy_cobblestone_stairs[facing=west,half=bottom,shape=straight,waterlogged=true]"
        IslandPalette.swap(IslandType.DESERTO, stairs) shouldBe
            "minecraft:sandstone_stairs[facing=west,half=bottom,shape=straight,waterlogged=true]"
        IslandPalette.swap(IslandType.GELO, stairs) shouldBe
            "minecraft:cobblestone_stairs[facing=west,half=bottom,shape=straight,waterlogged=true]"
    }

    "cacto fica no deserto e some no gelo" {
        IslandPalette.swap(IslandType.DESERTO, "minecraft:cactus[age=5]") shouldBe "minecraft:cactus[age=5]"
        IslandPalette.swap(IslandType.GELO, "minecraft:cactus[age=5]") shouldBe "minecraft:air"
    }

    "blocos fora da tabela ficam intactos" {
        val trapdoor = "minecraft:iron_trapdoor[facing=west,half=bottom,open=false,powered=false,waterlogged=false]"
        IslandType.entries.forEach { type ->
            IslandPalette.swap(type, "minecraft:lava[level=0]") shouldBe "minecraft:lava[level=0]"
            IslandPalette.swap(type, trapdoor) shouldBe trapdoor
            IslandPalette.swap(type, "minecraft:air") shouldBe "minecraft:air"
        }
    }

    "padrao e jangada nunca trocam" {
        listOf(IslandType.PADRAO, IslandType.JANGADA).forEach { type ->
            IslandPalette.swap(type, "minecraft:grass_block[snowy=false]") shouldBe "minecraft:grass_block[snowy=false]"
            IslandPalette.swap(type, "minecraft:oak_log[axis=y]") shouldBe "minecraft:oak_log[axis=y]"
        }
    }
})
