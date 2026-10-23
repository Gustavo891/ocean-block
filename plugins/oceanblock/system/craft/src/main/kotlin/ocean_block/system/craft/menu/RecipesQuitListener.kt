package ocean_block.system.craft.menu

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class RecipesQuitListener : Listener {

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        RecipeSelections.forget(event.player.uniqueId)
    }
}
