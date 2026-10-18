package ocean_block.system.drift.hook

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapedRecipe

object GanchoRecipe {

    private val KEY = NamespacedKey("oceanblock", "gancho")

    fun register() {
        if (Bukkit.getRecipe(KEY) != null) return

        val recipe = ShapedRecipe(KEY, GanchoItem.build())
            .shape("  P", " PS", "P S")
            .setIngredient('P', RecipeChoice.MaterialChoice(Tag.PLANKS))
            .setIngredient('S', Material.STRING)

        Bukkit.addRecipe(recipe)
    }
}
