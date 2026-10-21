package ocean_block.system.stations.smelter

import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.drift.collect.CollectorRule
import ocean_block.system.islands.island.IslandManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

class SmelterListener : Listener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK || event.hand != EquipmentSlot.HAND) return
        val block = event.clickedBlock ?: return
        val placed = PlacedBlockManager.get(block) ?: return
        SmelterBlock.of(placed.blockId) ?: return
        val player = event.player
        if (!player.isSneaking) return

        event.isCancelled = true
        val island = IslandManager.getIslandAt(block.location) ?: return
        if (!CollectorRule.canCollect(island, player)) return
        SmelterActions.use(player, block, placed, takeAll = true)
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onOpen(event: InventoryOpenEvent) {
        val location = event.inventory.location ?: return
        val placed = PlacedBlockManager.get(location.block) ?: return
        if (SmelterBlock.of(placed.blockId) != null) event.isCancelled = true
    }
}
