package one_block.system.fly_timer.listener

import one_block.system.fly_timer.Flys
import one_block.system.fly_timer.fly_player.FlyPlayer
import one_block.system.fly_timer.fly_player.FlyPlayerManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerToggleFlightEvent

class PlayerListener : Listener {

    @EventHandler
    fun onAsyncPlayerPreLogin(event: AsyncPlayerPreLoginEvent) {
        if (event.loginResult != AsyncPlayerPreLoginEvent.Result.ALLOWED) return

        val uuid = event.uniqueId
        val name = event.name.lowercase()

        val flyPlayer = FlyPlayerManager.repository.find(uuid) ?: FlyPlayer(uuid, name, 0)
        FlyPlayerManager.register(flyPlayer)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        FlyPlayerManager.unregister(player.uniqueId)
    }

    @EventHandler
    fun onToggleFlight(event: PlayerToggleFlightEvent) {
        val player = event.player
        val flyPlayer = FlyPlayerManager.get(player.uniqueId) ?: return

        if (event.isFlying) {
            // jogador começou a voar
            if (!flyPlayer.hasTime()) {
                event.isCancelled = true
                player.isFlying = false
                return
            }

            flyPlayer.startFlying()
        } else {
            // jogador parou de voar
            flyPlayer.stopFlying()
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item ?: return

        val timer = Flys.getTimer(item)
        if (timer == 0L) {
            return
        }

        val flyPlayer = FlyPlayerManager.get(player.uniqueId)
        if (flyPlayer == null) {
            player.sendRichMessage("<red>Jogador não carregado.</red>")
            return
        }

        flyPlayer.addSeconds(60)
        item.amount = 0

        player.sendRichMessage("<green>+1m de vôoW")
    }
}