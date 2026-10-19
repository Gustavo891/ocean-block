package ocean_block.system.craft.screen

import ocean_block.system.craft.recipe.RecipeCategory

data class RecipesView(
    val category: RecipeCategory? = null,
    val page: Int = 0,
)
