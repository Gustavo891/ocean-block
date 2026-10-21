package ocean_block.system.craft.recipe

import org.bukkit.inventory.ItemStack

data class OceanRecipe(
    val id: String,
    val name: String,
    val description: String,
    val category: RecipeCategory,
    val result: () -> ItemStack,
    val ingredients: List<Ingredient>,
    val research: String? = null,
) {

    init {
        require(id.isNotBlank()) { "receita sem id" }
        require(ingredients.isNotEmpty()) { "receita '$id' sem ingredientes" }
    }
}
