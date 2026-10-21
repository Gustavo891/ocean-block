package ocean_block.system.drift.recipe

import ocean_block.system.craft.recipe.Ingredient
import ocean_block.system.craft.recipe.OceanRecipe
import ocean_block.system.craft.recipe.RecipeCategory
import ocean_block.system.craft.recipe.Recipes
import ocean_block.system.drift.hook.GanchoItem
import ocean_block.system.drift.net.NetBlock
import org.bukkit.Material
import org.bukkit.Tag

object DriftRecipes {

    fun register() {
        Recipes.register(gancho())
        Recipes.register(rede())
    }

    private fun gancho() = OceanRecipe(
        id = "gancho",
        name = "Gancho",
        description = "Lance na água e recolha para puxar detritos.",
        category = RecipeCategory.FERRAMENTAS,
        result = { GanchoItem.build() },
        ingredients = listOf(tabuas(3), linhas(2)),
    )

    private fun rede() = OceanRecipe(
        id = NetBlock.ID,
        name = "Rede de Detritos",
        description = "Coleta sozinha os detritos que passam por perto.",
        category = RecipeCategory.COLETA,
        result = { NetBlock.buildItem() },
        ingredients = listOf(tabuas(4), linhas(5)),
        research = NetBlock.ID,
    )

    private fun tabuas(amount: Int) = Ingredient.tag(Tag.PLANKS, "Tábua", amount, plural = "Tábuas")

    private fun linhas(amount: Int) = Ingredient.material(Material.STRING, "Linha", amount, plural = "Linhas")
}
