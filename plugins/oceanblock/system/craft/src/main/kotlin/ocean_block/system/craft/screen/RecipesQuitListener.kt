package ocean_block.system.craft.screen

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class RecipesQuitListener(private val screen: RecipesScreen) : Listener {

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        screen.forget(event.player.uniqueId)
    }
}
