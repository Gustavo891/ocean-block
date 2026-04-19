package one_block.system.battle_pass.challenge

import one_block.system.battle_pass.bossbar.ChallengeBossbarManager
import one_block.system.battle_pass.config.Challenges
import one_block.system.battle_pass.player.BattlePassManager
import org.bukkit.entity.Player
import java.util.UUID

object ChallengeManager {

    fun getActiveChallenge(uuid: UUID): Challenges? =
        Challenges.byId(BattlePassManager.get(uuid).activeChallengeId)

    fun startChallenge(player: Player, challenge: Challenges): StartResult {
        val bp = BattlePassManager.get(player.uniqueId)

        val progress = ProgressManager.getProgress(player.uniqueId, challenge.id)
        if (progress?.completed == true) return StartResult.AlreadyCompleted

        val currentActive = bp.activeChallengeId
        if (currentActive != null && currentActive != challenge.id) {
            return StartResult.AlreadyActive(Challenges.byId(currentActive))
        }

        if (currentActive == challenge.id) {
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
        val active = Challenges.byId(bp.activeChallengeId) ?: return null
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

    sealed class StartResult {
        data object Started : StartResult()
        data object AlreadyCompleted : StartResult()
        data class AlreadyActive(val current: Challenges?) : StartResult()
    }
}
