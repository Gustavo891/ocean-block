package ocean_block.system.craft.recipe

import org.bukkit.entity.Player

object RecipeLocks {

    @Volatile
    private var provider: (Player, OceanRecipe) -> String? = { _, _ -> null }

    fun install(provider: (Player, OceanRecipe) -> String?) {
        this.provider = provider
    }

    fun lockedBy(player: Player, recipe: OceanRecipe): String? =
        if (recipe.research == null) null else provider(player, recipe)
}
