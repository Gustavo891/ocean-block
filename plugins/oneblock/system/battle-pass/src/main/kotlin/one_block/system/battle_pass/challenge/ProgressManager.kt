package one_block.system.battle_pass.challenge

import one_block.system.battle_pass.bossbar.ChallengeBossbarManager
import one_block.system.battle_pass.config.Challenges
import one_block.system.battle_pass.dao.ChallengeRepository
import one_block.system.battle_pass.player.BattlePassManager
import net.kyori.adventure.title.Title
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import one_block.libs.formatter.Mini

object ProgressManager {

    private val playerChallenges = ConcurrentHashMap<UUID, PlayerChallengeData>()
    private val repository = ChallengeRepository()

    fun onInit() {
        repository.toList().forEach { playerChallenges[it.playerUuid] = it }
    }

    private fun getPlayerData(playerUuid: UUID): PlayerChallengeData {
        return playerChallenges.getOrPut(playerUuid) {
            val data = PlayerChallengeData(playerUuid = playerUuid)
            repository.update(data)
            data
        }
    }

    fun getOrCreate(playerUuid: UUID, challenge: Challenges): ChallengeProgress {
        val data = getPlayerData(playerUuid)
        return data.progresses.getOrPut(challenge.id) {
            val p = ChallengeProgress(challengeId = challenge.id)
            repository.update(data)
            p
        }
    }

    fun increment(player: Player, challenge: Challenges, amount: Int = 1): ChallengeProgress {
        val data = getPlayerData(player.uniqueId)
        val progress = data.progresses.getOrPut(challenge.id) {
            ChallengeProgress(challengeId = challenge.id)
        }

        if (progress.completed) return progress

        progress.current = minOf(progress.current + amount, challenge.objective.goal)

        ChallengeBossbarManager.show(player, challenge, progress)

        if (progress.current >= challenge.objective.goal) {
            progress.completed = true
            if (!progress.xpClaimed) {
                BattlePassManager.addXp(player, challenge.xpReward)
                progress.xpClaimed = true
                player.showTitle(Title.title(
                    Mini.mm.deserialize("<green>Missão concluída!"),
                    Mini.mm.deserialize("<gray>${challenge.displayName} <dark_gray>[+${challenge.xpReward} XP]"),
                    Title.Times.times(Duration.ofMillis(100), Duration.ofSeconds(3), Duration.ofMillis(100))
                ))
                player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
            }
            ChallengeBossbarManager.hide(player)
            ChallengeManager.onActiveChallengeCompleted(player.uniqueId, challenge)
        }
        repository.update(data)
        return progress
    }

    fun getProgress(playerUuid: UUID, challengeId: String): ChallengeProgress? {
        return playerChallenges[playerUuid]?.progresses?.get(challengeId)
    }
}

