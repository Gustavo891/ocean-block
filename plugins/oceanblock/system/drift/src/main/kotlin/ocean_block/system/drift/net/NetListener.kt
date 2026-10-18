package ocean_block.system.drift.net

import me.devnatan.inventoryframework.ViewFrame
import ocean_block.system.custom_items.block.CustomBlockRegistry
import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.drift.collect.CollectorRule
import ocean_block.system.drift.net.view.NetStorageView
import ocean_block.system.islands.island.IslandManager
import org.bukkit.block.Block
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

class NetListener(private val viewFrame: ViewFrame) : Listener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        val placed = netAt(block) ?: return
        val player = event.player

        if (event.hand != EquipmentSlot.HAND || player.isSneaking) {
            event.setUseInteractedBlock(Event.Result.DENY)
            return
        }

        event.isCancelled = true
        val island = IslandManager.getIslandAt(block.location) ?: return
        if (!CollectorRule.canCollect(island, player)) return

        viewFrame.open(NetStorageView::class.java, player, placed)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlace(event: BlockPlaceEvent) {
        if (CustomBlockRegistry.fromItemStack(event.itemInHand) === NetBlock) NetIndex.rebuild()
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        if (event.block.type == NetBlock.material) NetIndex.rebuild()
    }

    private fun netAt(block: Block): PlacedBlock? {
        if (block.type != NetBlock.material) return null
        return PlacedBlockManager.get(block)?.takeIf { it.blockId == NetBlock.ID }
    }
}
