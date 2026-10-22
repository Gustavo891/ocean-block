package ocean_block.system.research.screen

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class ResearchQuitListener : Listener {

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        ResearchScreen.forget(event.player.uniqueId)
    }
}
