package ocean_block.system.drift.net

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapedRecipe

object NetRecipe {

    private val KEY = NamespacedKey("oceanblock", "rede")

    fun register() {
        if (Bukkit.getRecipe(KEY) != null) return

        val recipe = ShapedRecipe(KEY, NetBlock.buildItem())
            .shape("PSP", "SSS", "PSP")
            .setIngredient('P', RecipeChoice.MaterialChoice(Tag.PLANKS))
            .setIngredient('S', Material.STRING)

        Bukkit.addRecipe(recipe)
    }
}
