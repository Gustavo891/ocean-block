package ocean_block.system.machines.furnace

import org.bukkit.Bukkit
import org.bukkit.inventory.FurnaceRecipe
import org.bukkit.inventory.RecipeChoice

object FurnaceRecipes {

    @Volatile
    var results: Map<String, String> = emptyMap()
        private set

    fun load() {
        val loaded = mutableMapOf<String, String>()
        Bukkit.recipeIterator().forEach { recipe ->
            if (recipe !is FurnaceRecipe) return@forEach
            val choice = recipe.inputChoice as? RecipeChoice.MaterialChoice ?: return@forEach
            choice.choices.forEach { material -> loaded.putIfAbsent(material.name, recipe.result.type.name) }
        }
        results = loaded
    }
}
