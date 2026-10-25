package ocean_block.system.islands.generator

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent

class GeneratorBreakListener : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        if (!GeneratorBlocks.consume(BlockKey.of(event.block))) return
        Bukkit.getPluginManager().callEvent(GeneratorBlockBrokenEvent(event.player, event.block.type))
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlace(event: BlockPlaceEvent) {
        GeneratorBlocks.forget(BlockKey.of(event.block))
    }
}
