package ocean_block.system.research.menu

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class ResearchQuitListener : Listener {

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        ResearchSelections.forget(event.player.uniqueId)
    }
}
