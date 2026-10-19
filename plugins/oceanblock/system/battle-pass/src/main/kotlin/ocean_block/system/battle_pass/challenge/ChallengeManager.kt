package ocean_block.system.battle_pass.challenge

import ocean_block.system.battle_pass.bossbar.ChallengeBossbarManager
import ocean_block.system.battle_pass.config.Challenges
import ocean_block.system.battle_pass.player.BattlePassManager
import ocean_block.system.battle_pass.player.PlayerPassData
import org.bukkit.entity.Player
import java.util.UUID

object ChallengeManager {

    fun getActiveChallenge(uuid: UUID): Challenges? =
        activeOf(BattlePassManager.get(uuid))

    fun startChallenge(player: Player, challenge: Challenges): StartResult {
        val bp = BattlePassManager.get(player.uniqueId)

        val progress = ProgressManager.getProgress(player.uniqueId, challenge.id)
        if (progress?.completed == true) return StartResult.AlreadyCompleted

        val currentActive = activeOf(bp)
        if (currentActive != null && currentActive != challenge) {
            return StartResult.AlreadyActive(currentActive)
        }

        if (currentActive == challenge) {
            val p = ProgressManager.getOrCreate(player.uniqueId, challenge)
            ChallengeBossbarManager.show(player, challenge, p)
            return StartResult.Started
        }

        bp.activeChallengeId = challenge.id
        BattlePassManager.save(bp)

        val p = ProgressManager.getOrCreate(player.uniqueId, challenge)
        ChallengeBossbarManager.show(player, challenge, p)

        return StartResult.Started
    }

    fun cancelActiveChallenge(player: Player): Challenges? {
        val bp = BattlePassManager.get(player.uniqueId)
        val active = activeOf(bp) ?: return null
        bp.activeChallengeId = null
        BattlePassManager.save(bp)

        ChallengeBossbarManager.hide(player)

        return active
    }

    fun onActiveChallengeCompleted(uuid: UUID, challenge: Challenges) {
        val bp = BattlePassManager.get(uuid)
        if (bp.activeChallengeId == challenge.id) {
            bp.activeChallengeId = null
            BattlePassManager.save(bp)
        }
    }

    private fun activeOf(bp: PlayerPassData): Challenges? {
        val id = bp.activeChallengeId ?: return null
        val active = Challenges.byId(id)
        if (active == null) {
            bp.activeChallengeId = null
            BattlePassManager.save(bp)
        }
        return active
    }

    sealed class StartResult {
        data object Started : StartResult()
        data object AlreadyCompleted : StartResult()
        data class AlreadyActive(val current: Challenges?) : StartResult()
    }
}
