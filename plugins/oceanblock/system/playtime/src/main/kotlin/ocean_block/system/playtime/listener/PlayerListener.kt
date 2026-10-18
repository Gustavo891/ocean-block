package ocean_block.system.playtime.listener

import ocean_block.system.playtime.player_playtime.PlayerPlaytimeManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerListener : Listener {

    @EventHandler
    fun onAsyncPlayerPreLogin(event: AsyncPlayerPreLoginEvent) {
        if (event.loginResult != AsyncPlayerPreLoginEvent.Result.ALLOWED) return

        val uuid = event.uniqueId
        val name = event.name.lowercase()

        PlayerPlaytimeManager.load(uuid, name)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        PlayerPlaytimeManager.save(event.player.uniqueId)
    }
}
