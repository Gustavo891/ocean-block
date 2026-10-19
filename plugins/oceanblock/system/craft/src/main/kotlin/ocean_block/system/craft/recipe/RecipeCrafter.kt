package ocean_block.system.craft.recipe

import ocean_block.system.craft.events.CustomCraftEvent
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object RecipeCrafter {

    fun craft(player: Player, recipe: OceanRecipe): CraftResult {
        val inventory = player.inventory
        val contents = inventory.storageContents.toList()
        val remaining = IngredientMath.consume(contents, recipe.ingredients)
            ?: return CraftResult.Missing(IngredientMath.shortages(contents, recipe.ingredients))

        val item = recipe.result()
        inventory.storageContents = remaining.toTypedArray()
        deliver(player, item.clone())
        Bukkit.getPluginManager().callEvent(CustomCraftEvent(player, recipe.id, item.amount))
        player.playSound(player.location, Sound.BLOCK_SMITHING_TABLE_USE, 0.8f, 1.2f)
        return CraftResult.Crafted(item)
    }

    fun shortages(player: Player, recipe: OceanRecipe): List<Shortage> =
        IngredientMath.shortages(player.inventory.storageContents.toList(), recipe.ingredients)

    fun count(player: Player, ingredient: Ingredient): Int =
        IngredientMath.count(player.inventory.storageContents.toList(), ingredient)

    private fun deliver(player: Player, item: ItemStack) {
        player.inventory.addItem(item).values.forEach { leftover ->
            player.world.dropItemNaturally(player.location, leftover)
        }
    }
}
