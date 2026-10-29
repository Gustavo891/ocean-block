package ocean_block.system.islands.island.type

import ocean_block.system.islands.island.IslandManager
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockFormEvent

class IceGuardListener : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onForm(event: BlockFormEvent) {
        if (event.newState.type != Material.ICE) return
        val island = IslandManager.getIslandAt(event.block.location) ?: return
        if (island.type() == IslandType.GELO) event.isCancelled = true
    }
}
