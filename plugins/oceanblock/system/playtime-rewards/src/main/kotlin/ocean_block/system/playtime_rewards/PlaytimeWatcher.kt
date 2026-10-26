package ocean_block.system.playtime_rewards

import ocean_block.system.playtime_rewards.notify.RewardNotifier
import ocean_block.system.playtime_rewards.rules.MilestoneRules
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

class PlaytimeWatcher(private val plugin: JavaPlugin) : Listener {

    private val lastSeen = mutableMapOf<UUID, Long>()

    fun start() {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable { Bukkit.getOnlinePlayers().forEach(::check) }, CHECK_TICKS, CHECK_TICKS)
        Bukkit.getOnlinePlayers().forEach { player -> PlaytimeRewards.seconds(player)?.let { lastSeen[player.uniqueId] = it } }
    }

    private fun check(player: Player) {
        val now = PlaytimeRewards.seconds(player) ?: return
        val before = lastSeen.put(player.uniqueId, now) ?: return
        MilestoneRules.reached(PlaytimeRewards.ladder, before, now, PlaytimeRewards.claimed(player))
            .forEach { RewardNotifier.reached(player, it) }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            if (!player.isOnline) return@Runnable
            PlaytimeRewards.seconds(player)?.let { lastSeen[player.uniqueId] = it }
            RewardNotifier.pending(player, PlaytimeRewards.available(player).size)
        }, JOIN_DELAY_TICKS)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        lastSeen.remove(event.player.uniqueId)
    }

    private companion object {
        const val CHECK_TICKS = 400L
        const val JOIN_DELAY_TICKS = 60L
    }
}
