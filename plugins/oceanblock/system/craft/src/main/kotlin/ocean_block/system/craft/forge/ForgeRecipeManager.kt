package ocean_block.system.craft.forge

import ocean_block.system.craft.CraftCategory
import ocean_block.system.craft.CraftStation
import org.bukkit.entity.Player

object ForgeRecipeManager {

    val recipes = mutableListOf<ForgeRecipe>()

    private val unlocks = mutableMapOf<String, RecipeUnlock>()

    fun addRecipe(recipe: ForgeRecipe) {
        recipes += recipe
    }

    fun gate(recipeId: String, unlock: RecipeUnlock) {
        unlocks[recipeId] = unlock
    }

    fun unlockOf(recipeId: String): RecipeUnlock? = unlocks[recipeId]

    fun isUnlocked(player: Player, recipe: ForgeRecipe): Boolean =
        unlocks[recipe.id]?.isUnlocked(player) ?: true

    fun requirementLines(player: Player, recipe: ForgeRecipe): List<String> =
        unlocks[recipe.id]?.requirementLines(player) ?: emptyList()

    fun byCategory(category: CraftCategory): List<ForgeRecipe> =
        recipes.filter { it.category == category }

    fun categoriesOf(station: CraftStation): List<CraftCategory> =
        CraftCategory.entries.filter { it.station == station && byCategory(it).isNotEmpty() }
}
