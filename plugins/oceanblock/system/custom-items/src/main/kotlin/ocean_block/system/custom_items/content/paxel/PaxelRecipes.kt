package ocean_block.system.custom_items.content.paxel

import ocean_block.system.custom_items.item.CustomItemRegistry
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.plugin.java.JavaPlugin

class PaxelRecipes(private val plugin: JavaPlugin) : Listener {

    private val keys = mutableSetOf<NamespacedKey>()

    fun register(paxels: List<PaxelItem>) {
        paxels.forEach { paxel ->
            val key = NamespacedKey(plugin, PaxelRecipeRules.key(paxel.id))
            Bukkit.removeRecipe(key)
            val recipe = ShapedRecipe(key, paxel.build(1)).shape(*PaxelRecipeRules.SHAPE.toTypedArray())
            PaxelRecipeRules.ingredients(paxel.material.name).forEach { (symbol, name) ->
                recipe.setIngredient(symbol, Material.valueOf(name))
            }
            if (Bukkit.addRecipe(recipe)) keys += key
            else plugin.logger.warning("[paxel] receita ${key.key} não registrada")
        }
    }

    @EventHandler
    fun onPrepare(event: PrepareItemCraftEvent) {
        val key = (event.recipe as? ShapedRecipe)?.key ?: return
        if (key !in keys) return
        val customIds = event.inventory.matrix.map { item -> item?.let { CustomItemRegistry.fromItemStack(it)?.id } }
        if (!PaxelRecipeRules.allowsMatrix(customIds)) event.inventory.result = null
    }
}
