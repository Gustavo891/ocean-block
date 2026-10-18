package ocean_block.system.collections.listener

import ocean_block.system.collections.collection.CollectionManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerListener : Listener {

    @EventHandler
    fun onAsyncPlayerPreLogin(event: AsyncPlayerPreLoginEvent) {
        if (event.loginResult != AsyncPlayerPreLoginEvent.Result.ALLOWED) return

        CollectionManager.load(event.uniqueId, event.name)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        CollectionManager.unload(event.player.uniqueId)
    }
}
