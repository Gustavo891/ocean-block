package ocean_block.system.level.menu

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import ocean_block.system.craft.recipe.Ingredient
import ocean_block.system.craft.recipe.OceanRecipe
import ocean_block.system.craft.recipe.RecipeCategory
import ocean_block.system.level.config.LevelConfig
import ocean_block.system.level.curve.LevelCurve
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

private fun recipe(level: Int) = OceanRecipe(
    id = "fundicao",
    name = "Fundição",
    description = "Derrete minérios",
    category = RecipeCategory.PROCESSAMENTO,
    result = { ItemStack(Material.FURNACE) },
    ingredients = listOf(Ingredient("Pedregulho", 8) { true }),
    level = level,
)

class LevelLoreTest : StringSpec({

    "cabecalho mostra nivel, XP no nivel e percentual" {
        val header = LevelLore.header(150)

        header["nivel"] shouldBe "Nível 2"
        header["xp"] shouldBe "50 / 283 XP"
        header["pct"] shouldBe "17,67%"
        header["barra"] shouldBe "17"
    }

    "no nivel maximo o cabecalho avisa e enche a barra" {
        val header = LevelLore.header(LevelCurve.totalFor(LevelCurve.MAX_LEVEL))

        header["nivel"] shouldBe "Nível 50"
        header["xp"] shouldBe "Nível máximo"
        header["pct"] shouldBe "100%"
        header["barra"] shouldBe "100"
    }


    "fontes de XP usam as faixas do nivel.yml padrao" {
        val sources = LevelLore.sources(LevelConfig.DEFAULT, listOf(1, 3, 8, 40))

        sources.map { it["texto"] } shouldContainExactly listOf(
            "Detritos: 4 a 25 XP",
            "Gerador: 1 a 20 XP",
            "Receitas: 13 a 130 XP",
        )
        sources.map { it["icone"] } shouldContainExactly listOf("fishing_rod", "cobblestone", "crafting_table")
    }

    "faixa de um valor so mostra um numero" {
        val config = LevelConfig.DEFAULT.copy(debrisDefault = 5, debris = mapOf("sucata" to 5))

        LevelLore.sources(config, emptyList()).map { it["texto"] } shouldContainExactly listOf(
            "Detritos: 5 XP",
            "Gerador: 1 a 20 XP",
            "Receitas: 13 XP",
        )
    }
})
