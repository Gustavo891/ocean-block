package ocean_block.system.level.curve

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import ocean_block.system.craft.recipe.Ingredient
import ocean_block.system.craft.recipe.OceanRecipe
import ocean_block.system.craft.recipe.RecipeCategory
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

private fun recipe(id: String, level: Int) = OceanRecipe(
    id = id,
    name = id,
    description = id,
    category = RecipeCategory.FERRAMENTAS,
    result = { ItemStack(Material.STICK) },
    ingredients = listOf(Ingredient("Tábua", 1, "Tábuas") { true }),
    level = level,
)

class LevelUpsTest : StringSpec({

    val recipes = listOf(recipe("doze", 12), recipe("um", 1), recipe("tres", 3), recipe("oito", 8))

    "subir um nivel libera so as receitas daquele nivel" {
        LevelUps.unlocked(2, 3, recipes).map { it.id } shouldContainExactly listOf("tres")
    }

    "subir varios niveis libera tudo do intervalo em ordem de nivel" {
        LevelUps.unlocked(1, 12, recipes).map { it.id } shouldContainExactly listOf("tres", "oito", "doze")
    }

    "sem subir ou acima de tudo nao libera nada" {
        LevelUps.unlocked(5, 5, recipes).shouldBeEmpty()
        LevelUps.unlocked(12, 50, recipes).shouldBeEmpty()
    }
})
