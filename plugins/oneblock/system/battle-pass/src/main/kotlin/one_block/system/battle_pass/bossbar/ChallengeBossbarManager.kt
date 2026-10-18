package one_block.system.battle_pass.bossbar

import net.kyori.adventure.bossbar.BossBar
import one_block.system.battle_pass.challenge.ChallengeProgress
import one_block.system.battle_pass.config.Challenges
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import one_block.libs.formatter.Mini

object ChallengeBossbarManager {

    private val activeBars = ConcurrentHashMap<UUID, BossBar>()

    fun show(player: Player, challenge: Challenges, progress: ChallengeProgress) {
        val bar = activeBars.getOrPut(player.uniqueId) {
            val newBar = BossBar.bossBar(
                formatName(challenge, progress),
                calculateProgress(challenge, progress),
                BossBar.Color.GREEN,
                BossBar.Overlay.PROGRESS
            )
            player.showBossBar(newBar)
            newBar
        }

        bar.name(formatName(challenge, progress))
        bar.progress(calculateProgress(challenge, progress))
    }

    fun hide(player: Player) {
        activeBars.remove(player.uniqueId)?.let { bar ->
            player.hideBossBar(bar)
        }
    }

    private fun formatName(challenge: Challenges, progress: ChallengeProgress): net.kyori.adventure.text.Component {
        val goal = challenge.objective.goal
        val current = progress.current
        return Mini.mm.deserialize(
            "<white>Missão: <gray>${challenge.displayName} <dark_gray>[$current/$goal]"
        )
    }

    private fun calculateProgress(challenge: Challenges, progress: ChallengeProgress): Float {
        val goal = challenge.objective.goal
        if (goal <= 0) return 1f
        return (progress.current.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    }
}
