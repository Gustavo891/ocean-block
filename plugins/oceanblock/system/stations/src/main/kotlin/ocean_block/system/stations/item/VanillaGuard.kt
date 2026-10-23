package ocean_block.system.stations.item

import ocean_block.system.custom_items.block.PlacedBlockManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.inventory.Inventory

class VanillaGuard : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onMove(event: InventoryMoveItemEvent) {
        if (isStation(event.source) || isStation(event.destination)) event.isCancelled = true
    }

    private fun isStation(inventory: Inventory): Boolean {
        val location = inventory.location ?: return false
        return StationBlockIds.isStation(PlacedBlockManager.get(location.block)?.blockId)
    }
}
