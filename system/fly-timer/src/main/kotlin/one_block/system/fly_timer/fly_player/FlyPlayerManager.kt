package one_block.system.fly_timer.fly_player

import net.kyori.adventure.text.minimessage.MiniMessage
import one_block.libs.instance.Instance
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
            if (player.hasTime()){
                bukkit.sendActionBar(MiniMessage.miniMessage().deserialize("<yellow>Tempo de vôo ${player.getRemainingSeconds1()}s"))
                continue
            }

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
        val values = players.values
        repository.updateAll(values.toList())
    }
}