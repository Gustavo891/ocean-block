package ocean_block.system.machines.furnace

import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.machines.MachineIds
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.inventory.Inventory

class FurnaceHopperGuard : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onMove(event: InventoryMoveItemEvent) {
        if (isFurnace(event.source) || isFurnace(event.destination)) event.isCancelled = true
    }

    private fun isFurnace(inventory: Inventory): Boolean {
        val location = inventory.location ?: return false
        return PlacedBlockManager.get(location.block)?.blockId == MachineIds.FURNACE
    }
}
