package ocean_block.system.skills.reward

import ocean_block.libs.rewards.RewardGranter
import ocean_block.system.currencies.Currencies
import ocean_block.system.skills.skill.Skill
import ocean_block.system.skills.skill.SkillData
import ocean_block.system.skills.skill.SkillLevelReward
import ocean_block.system.skills.skill.SkillManager
import ocean_block.system.skills.skill.SkillRewards
import org.bukkit.entity.Player

object SkillRewarder {

    fun grantUpTo(player: Player, data: SkillData, skill: Skill) {
        val current = data.levelOf(skill)
        val alreadyPaid = data.rewardedLevelOf(skill)

        if (current <= alreadyPaid) return

        for (level in (alreadyPaid + 1)..current) {
            val reward = SkillRewards.at(skill, level) ?: continue
            pay(player, skill, reward)
        }

        data.markRewarded(skill, current)
        SkillManager.markDirty(player.uniqueId)
    }

    private fun pay(player: Player, skill: Skill, reward: SkillLevelReward) {
        if (reward.coins > 0.0) {
            Currencies.deposit(player.uniqueId, "coins", reward.coins)
        }

        RewardGranter.grant(reward.rewards, player)

        player.sendRichMessage(
            "<gold>Recompensa de ${skill.prettyName} <white>${reward.level}<gold>: ${summary(reward)}"
        )
    }

    private fun summary(reward: SkillLevelReward): String = buildList {
        if (reward.coins > 0.0) add("<yellow>${reward.coins.toLong()} coins")
        reward.rewards.forEach { item ->
            val material = item.material ?: return@forEach
            add("<white>${item.amount ?: 1}x ${material.name.lowercase().replace('_', ' ')}")
        }
    }.joinToString("<gray>, ")
}
