package one_block.system.currencies.listener

import one_block.system.currencies.currency.CurrencyManager
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

        CurrencyManager.toList().forEach { it.load(uuid, name) }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        CurrencyManager.toList().forEach { it.save(player.uniqueId) }
    }
}