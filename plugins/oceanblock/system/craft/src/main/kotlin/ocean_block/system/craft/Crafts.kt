package ocean_block.system.craft

import ocean_block.system.craft.forge.ForgeRecipe
import ocean_block.system.craft.forge.ForgeRecipeManager
import ocean_block.system.craft.forge.RecipeUnlock

object Crafts {

    fun register(forgeRecipe: ForgeRecipe) = ForgeRecipeManager.addRecipe(forgeRecipe)

    fun gate(recipeId: String, unlock: RecipeUnlock) = ForgeRecipeManager.gate(recipeId, unlock)
}
