package ocean_block.system.level.dialog

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

    "receita acima do nivel do jogador fica bloqueada" {
        val entry = LevelLore.recipeEntry(recipe(8), 7, "furnace")

        entry["item"] shouldBe "furnace"
        entry["nome"] shouldBe "Fundição"
        entry["nivel"] shouldBe "Nível 8"
        entry["estado"] shouldBe "✖ Bloqueada"
        entry["estado_estilo"] shouldBe "bad"
        entry["situacao"] shouldBe "Bloqueada"
        entry["bloqueada"] shouldBe "true"
    }

    "receita do nivel do jogador fica liberada" {
        val entry = LevelLore.recipeEntry(recipe(8), 8, "furnace")

        entry["estado"] shouldBe "✔ Liberada"
        entry["estado_estilo"] shouldBe "good"
        entry["situacao"] shouldBe "Liberada"
        entry["bloqueada"] shouldBe "false"
    }

    "fontes de XP usam as faixas do nivel.yml padrao" {
        val sources = LevelLore.sources(LevelConfig.DEFAULT, listOf(1, 3, 8, 40))

        sources.map { it["texto"] } shouldContainExactly listOf(
            "Detritos: 3 a 10 XP",
            "Gerador: 1 a 20 XP",
            "Receitas: 13 a 130 XP",
        )
        sources.map { it["icone"] } shouldContainExactly listOf("fishing_rod", "cobblestone", "crafting_table")
    }

    "faixa de um valor so mostra um numero" {
        val config = LevelConfig.DEFAULT.copy(debrisDefault = 5, debris = mapOf("tabua" to 5))

        LevelLore.sources(config, emptyList()).map { it["texto"] } shouldContainExactly listOf(
            "Detritos: 5 XP",
            "Gerador: 1 a 20 XP",
            "Receitas: 13 XP",
        )
    }
})
