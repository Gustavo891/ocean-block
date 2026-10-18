package one_block.system.fly_timer.fly_player

import one_block.libs.instance.Instance
import one_block.system.fly_timer.hud.FlyHud
import one_block.system.fly_timer.listener.PlayerListener.Companion.ALLOWED_GAMEMODE
import org.bukkit.Bukkit
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object FlyPlayerManager {

    private val players = mutableMapOf<UUID, FlyPlayer>()
    val repository = FlyPlayerRepository()

    fun onInit() {
        val service = Executors.newSingleThreadScheduledExecutor()
        service.scheduleAtFixedRate({ updatePlayers() }, 0, 1, TimeUnit.MINUTES)

        Bukkit.getScheduler().runTaskTimer(Instance, Runnable {
            processPlayers()
        }, 20L, 20L)
    }

    fun onDisable() = updatePlayers()

    fun register(flyPlayer: FlyPlayer) {
        players[flyPlayer.uuid] = flyPlayer
    }

    fun unregister(uuid: UUID) {
        val flyPlayer = players.remove(uuid) ?: return
        repository.update(flyPlayer)
    }

    fun get(uuid: UUID) = players[uuid]

    private fun processPlayers() {
        for (player in players.values) {
            val bukkit = Bukkit.getPlayer(player.uuid) ?: continue
            if (player.hasTime()) {
                FlyHud.show(bukkit, player.getRemainingSeconds1())
                continue
            }

            FlyHud.hide(bukkit)

            if (bukkit.gameMode in ALLOWED_GAMEMODE) continue
            if (!bukkit.isFlying) continue

            if (!player.hasTime()) {
                player.stopFlying()
                bukkit.isFlying = false
                bukkit.allowFlight = false

                bukkit.sendRichMessage("fly acabou")
            }

        }
    }

    private fun updatePlayers() {
        repository.updateAll(players.values.toList())
    }
}
