package ocean_block.system.playtime_rewards.dialog

import ocean_block.system.playtime_rewards.model.Milestone
import ocean_block.system.playtime_rewards.rules.MilestoneRules
import ocean_block.system.playtime_rewards.rules.MilestoneStatus
import ocean_block.system.playtime_rewards.text.RewardText

object RewardRows {

    private val ANIMATED = setOf("clock", "compass", "recovery_compass")

    fun iconTexture(icon: String): String = if (icon in ANIMATED) "${icon}_00" else icon

    fun row(milestone: Milestone, seconds: Long, claimed: Set<String>, itemName: (String) -> String): Map<String, String> {
        val status = MilestoneRules.status(milestone, seconds, claimed)
        val (label, style) = when (status) {
            MilestoneStatus.Claimed -> "Resgatado" to "good"
            MilestoneStatus.Available -> "Liberado!" to "accent"
            is MilestoneStatus.Locked -> "Faltam ${RewardText.remaining(status.missingSeconds)}" to "muted"
        }
        val prizes = RewardText.prizes(milestone, itemName)
        return mapOf(
            "icone" to "item:${iconTexture(milestone.icon)}",
            "nome" to RewardText.duration(milestone.minutes),
            "premios" to prizes,
            "situacao" to label,
            "situacao_estilo" to style,
            "barra" to MilestoneRules.progress(milestone, seconds).toString(),
            "bloqueado" to (status != MilestoneStatus.Available).toString(),
            "dica" to "${RewardText.duration(milestone.minutes)} jogadas\n\n$prizes",
        )
    }
}
