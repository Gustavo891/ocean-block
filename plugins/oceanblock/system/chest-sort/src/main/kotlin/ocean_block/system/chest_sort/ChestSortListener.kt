package ocean_block.system.chest_sort

import ocean_block.system.islands.island.IslandManager
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.block.Barrel
import org.bukkit.block.Chest
import org.bukkit.block.DoubleChest
import org.bukkit.block.ShulkerBox
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory

class ChestSortListener : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onSwapOffhand(event: InventoryClickEvent) {
        if (event.click != ClickType.SWAP_OFFHAND) return
        val player = event.whoClicked as? Player ?: return
        val top = event.view.topInventory
        if (event.clickedInventory != top) return
        val location = containerLocation(top) ?: return

        event.isCancelled = true
        if (!canSort(player, location)) return

        top.contents = ChestSortRules.sort(top.contents.toList(), ItemStackKind).toTypedArray()
        player.playSound(player, Sound.ITEM_BUNDLE_INSERT, 0.8f, 1.0f)
    }

    private fun containerLocation(inventory: Inventory): Location? =
        when (val holder = inventory.getHolder(false)) {
            is DoubleChest -> holder.location
            is Chest -> holder.location
            is Barrel -> holder.location
            is ShulkerBox -> holder.location
            else -> null
        }

    private fun canSort(player: Player, location: Location): Boolean {
        val island = IslandManager.getIslandAt(location) ?: return true
        return island.isTrustAllowed(player)
    }
}
