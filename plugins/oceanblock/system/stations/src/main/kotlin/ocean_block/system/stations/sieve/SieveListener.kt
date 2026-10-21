package ocean_block.system.stations.sieve

import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.drift.collect.CollectorRule
import ocean_block.system.islands.island.IslandManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot

class SieveListener : Listener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK || event.hand != EquipmentSlot.HAND) return
        val block = event.clickedBlock ?: return
        val placed = PlacedBlockManager.get(block) ?: return
        val sieve = SieveBlock.of(placed.blockId) ?: return
        val player = event.player
        if (!player.isSneaking) return

        event.isCancelled = true
        val island = IslandManager.getIslandAt(block.location) ?: return
        if (!CollectorRule.canCollect(island, player)) return
        if (sieve.tier == SieveTier.AUTOMATIC) return
        SieveActions.use(player, block, placed, sieve.tier, quick = true)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        SieveActions.forget(event.player.uniqueId)
    }
}
