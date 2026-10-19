package ocean_block.system.drift.collect

import one_block.textura.api.NotificationApi
import one_block.textura.notify.NotificationLine
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID

object CollectNotifier : Listener {

    private const val REGION = "coleta"
    private const val DURATION_TICKS = 60

    private val tallies = mutableMapOf<UUID, CollectTally>()

    fun notify(player: Player, material: Material, amount: Int) {
        val tally = tallies.getOrPut(player.uniqueId) { CollectTally(DURATION_TICKS.toLong()) }
        val total = tally.add(material.name, amount, Bukkit.getCurrentTick().toLong())
        NotificationApi.push(player, REGION, line(material, total))
    }

    fun iconOf(material: Material): String = "item:${material.key.key}"

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        tallies.remove(event.player.uniqueId)
    }

    private fun line(material: Material, total: Int): NotificationLine =
        NotificationLine(
            "<green>+$total</green> <white><lang:${material.translationKey()}></white>",
            iconOf(material),
            DURATION_TICKS,
            "coleta:${material.name}",
        )
}
