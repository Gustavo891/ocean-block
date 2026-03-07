package one_block.system.craft.forge

 object ForgeRecipeManager {

    val recipes = mutableListOf<ForgeRecipe>()

    fun addRecipe(recipe: ForgeRecipe) {
        recipes += recipe
    }

}