package one_block.system.craft

import one_block.system.craft.forge.ForgeRecipe
import one_block.system.craft.forge.ForgeRecipeManager

object Crafts {

    fun register(forgeRecipe: ForgeRecipe) = ForgeRecipeManager.addRecipe(forgeRecipe)
}