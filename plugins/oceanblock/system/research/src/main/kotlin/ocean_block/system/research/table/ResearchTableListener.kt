package ocean_block.system.research.table

import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.drift.collect.CollectorRule
import ocean_block.system.islands.island.IslandManager
import ocean_block.system.research.screen.ResearchScreen
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot

class ResearchTableListener : Listener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK || event.hand != EquipmentSlot.HAND) return
        val block = event.clickedBlock ?: return
        val placed = PlacedBlockManager.get(block) ?: return
        if (placed.blockId != ResearchTableBlock.ID || !event.player.isSneaking) return

        event.isCancelled = true
        val island = IslandManager.getIslandAt(block.location) ?: return
        if (!CollectorRule.canCollect(island, event.player)) return
        ResearchScreen.open(event.player, island.uuid)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        ResearchScreen.forget(event.player.uniqueId)
    }
}
