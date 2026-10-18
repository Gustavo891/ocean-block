package ocean_block.system.battle_pass.listener

import ocean_block.system.battle_pass.bossbar.ChallengeBossbarManager
import ocean_block.system.battle_pass.challenge.ChallengeManager
import ocean_block.system.battle_pass.challenge.ProgressManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerListener : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        val active = ChallengeManager.getActiveChallenge(player.uniqueId) ?: return
        val progress = ProgressManager.getOrCreate(player.uniqueId, active)

        ChallengeBossbarManager.show(player, active, progress)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        ChallengeBossbarManager.hide(event.player)
    }
}
