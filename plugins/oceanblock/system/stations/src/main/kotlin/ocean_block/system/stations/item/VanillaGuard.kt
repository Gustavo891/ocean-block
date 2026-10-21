package ocean_block.system.stations.item

import ocean_block.system.custom_items.CustomIds
import ocean_block.system.custom_items.block.PlacedBlockManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockCookEvent
import org.bukkit.event.inventory.FurnaceStartSmeltEvent
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.inventory.Inventory

class VanillaGuard : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onCook(event: BlockCookEvent) {
        if (isPiece(CustomIds.of(event.source))) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onStartSmelt(event: FurnaceStartSmeltEvent) {
        if (isPiece(CustomIds.of(event.source))) event.totalCookTime = Int.MAX_VALUE
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPrepareCraft(event: PrepareItemCraftEvent) {
        if (event.inventory.matrix.any { isPiece(CustomIds.of(it)) }) event.inventory.result = null
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onMove(event: InventoryMoveItemEvent) {
        if (isStation(event.source) || isStation(event.destination)) event.isCancelled = true
    }

    private fun isStation(inventory: Inventory): Boolean {
        val location = inventory.location ?: return false
        return StationBlockIds.isStation(PlacedBlockManager.get(location.block)?.blockId)
    }

    private fun isPiece(id: String?): Boolean = id != null && OrePiece.byId(id) != null
}
