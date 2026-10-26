package ocean_block.system.playtime_rewards

import ocean_block.libs.rewards.Reward
import ocean_block.libs.rewards.RewardGranter
import ocean_block.libs.rewards.RewardType
import ocean_block.system.currencies.Currencies
import ocean_block.system.level.PlayerLevels
import ocean_block.system.level.data.LevelManager
import ocean_block.system.playtime.player_playtime.PlayerPlaytimeManager
import ocean_block.system.playtime_rewards.data.PlaytimeRewardManager
import ocean_block.system.playtime_rewards.model.Milestone
import ocean_block.system.playtime_rewards.rules.ClaimResult
import ocean_block.system.playtime_rewards.rules.MilestoneRules
import ocean_block.system.playtime_rewards.text.RewardText
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player

object PlaytimeRewards {

    @Volatile
    var ladder: List<Milestone> = emptyList()
        internal set

    fun seconds(player: Player): Long? = PlayerPlaytimeManager.get(player.uniqueId)?.getElapsedSeconds()

    fun claimed(player: Player): Set<String> = PlaytimeRewardManager.claimed(player.uniqueId)

    fun available(player: Player): List<Milestone> {
        val seconds = seconds(player) ?: return emptyList()
        return MilestoneRules.available(ladder, seconds, claimed(player))
    }

    fun claim(player: Player, id: String) {
        val milestone = ladder.firstOrNull { it.id == id } ?: return player.sendRichMessage("<red>Recompensa não encontrada.")
        val seconds = seconds(player) ?: return player.sendRichMessage("<red>Seu tempo jogado ainda não carregou.")
        when (val result = grant(player, milestone, seconds)) {
            is ClaimResult.Claimed -> celebrate(player, "Recompensa de ${RewardText.duration(milestone.minutes)} resgatada!")
            ClaimResult.AlreadyClaimed -> player.sendRichMessage("<red>Você já resgatou essa recompensa.")
            is ClaimResult.NotReached ->
                player.sendRichMessage("<red>Faltam ${RewardText.remaining(result.missingSeconds)} para essa recompensa.")
        }
    }

    fun claimAll(player: Player) {
        val seconds = seconds(player) ?: return player.sendRichMessage("<red>Seu tempo jogado ainda não carregou.")
        val granted = available(player).count { grant(player, it, seconds) is ClaimResult.Claimed }
        if (granted == 0) return player.sendRichMessage("<red>Nenhuma recompensa para resgatar.")
        celebrate(player, if (granted == 1) "1 recompensa resgatada!" else "$granted recompensas resgatadas!")
    }

    private fun grant(player: Player, milestone: Milestone, seconds: Long): ClaimResult {
        val result = MilestoneRules.claim(milestone, seconds, claimed(player))
        if (result is ClaimResult.Claimed) {
            PlaytimeRewardManager.setClaimed(player.uniqueId, result.claimed)
            pay(player, milestone)
        }
        return result
    }

    private fun celebrate(player: Player, message: String) {
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.4f)
        player.sendRichMessage("<green>$message")
    }

    fun reset(player: Player) {
        PlaytimeRewardManager.setClaimed(player.uniqueId, emptySet())
    }

    private fun pay(player: Player, milestone: Milestone) {
        val uuid = player.uniqueId
        if (milestone.coins > 0) Currencies.deposit(uuid, "coins", milestone.coins.toDouble())
        if (milestone.pearls > 0) Currencies.deposit(uuid, "perolas", milestone.pearls.toDouble())
        giveXp(player, milestone.xp)
        val rewards = milestone.items.mapNotNull { prize ->
            Material.matchMaterial(prize.material)?.let { Reward(RewardType.Item, material = it, amount = prize.amount) }
        } + milestone.commands.map { Reward(RewardType.Command, value = it) }
        RewardGranter.grant(rewards, player)
    }

    private fun giveXp(player: Player, amount: Int) {
        if (amount <= 0) return
        if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) {
            PlayerLevels.setXp(player, LevelManager.xp(player.uniqueId) + amount)
        } else {
            PlayerLevels.award(player, amount)
        }
    }
}
