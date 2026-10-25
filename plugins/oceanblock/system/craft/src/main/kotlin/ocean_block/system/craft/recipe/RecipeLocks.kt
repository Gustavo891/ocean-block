package ocean_block.system.craft.recipe

import org.bukkit.entity.Player

object RecipeLocks {

    @Volatile
    private var provider: ((Player) -> Int)? = null

    fun install(provider: (Player) -> Int) {
        this.provider = provider
    }

    fun requiredLevel(player: Player, recipe: OceanRecipe): Int? =
        required(recipe.level, provider?.invoke(player))

    fun required(recipeLevel: Int, playerLevel: Int?): Int? =
        if (playerLevel == null || playerLevel >= recipeLevel) null else recipeLevel
}
