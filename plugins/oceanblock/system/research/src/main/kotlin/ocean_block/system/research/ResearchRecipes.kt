package ocean_block.system.research

import ocean_block.system.craft.recipe.Ingredient
import ocean_block.system.craft.recipe.OceanRecipe
import ocean_block.system.craft.recipe.RecipeCategory
import ocean_block.system.craft.recipe.Recipes
import ocean_block.system.custom_items.CustomIds
import ocean_block.system.research.table.ResearchTableBlock
import org.bukkit.Material
import org.bukkit.Tag

object ResearchRecipes {

    fun register() {
        Recipes.register(
            OceanRecipe(
                id = ResearchTableBlock.ID,
                name = "Mesa de Pesquisa",
                description = "Libera estações e melhorias para o time do raft.",
                category = RecipeCategory.PROCESSAMENTO,
                result = { ResearchTableBlock.buildItem() },
                ingredients = listOf(
                    Ingredient.tag(Tag.PLANKS, "Tábua", 6, plural = "Tábuas"),
                    plain(Material.STRING, "Linha", 3, "Linhas"),
                    plain(Material.IRON_NUGGET, "Pepita de ferro", 2, "Pepitas de ferro"),
                ),
            ),
        )
    }

    private fun plain(material: Material, label: String, amount: Int, plural: String) =
        Ingredient(label, amount, plural) { it.type == material && CustomIds.of(it) == null }
}
