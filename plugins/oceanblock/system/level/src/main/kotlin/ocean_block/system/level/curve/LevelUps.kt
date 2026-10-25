package ocean_block.system.level.curve

import ocean_block.system.craft.recipe.OceanRecipe

object LevelUps {

    fun unlocked(from: Int, to: Int, recipes: List<OceanRecipe>): List<OceanRecipe> =
        recipes.filter { it.level in (from + 1)..to }.sortedBy { it.level }
}
