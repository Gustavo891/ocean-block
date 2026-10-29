package ocean_block.system.missions.text

import ocean_block.system.missions.catalog.DailyPool
import ocean_block.system.missions.model.Mission
import ocean_block.system.missions.model.MissionObjective
import ocean_block.system.missions.rules.MissionRules

data class MissionCardText(
    val topLeft: String,
    val topRight: String,
    val bottomRight: String,
    val progress: Float,
)

object MissionCard {

    fun current(current: MissionRules.Current?, trailDone: Boolean, recipeName: (String) -> String): MissionCardText? {
        if (current == null) {
            if (!trailDone) return null
            val all = DailyPool.DAILY_COUNT
            return MissionCardText("<gray>Diárias concluídas. Volte amanhã!</gray>", "", "<green>$all/$all</green>", 1f)
        }
        val objective = current.mission.objective
        val corner = current.dailyIndex?.let { "<gray>Diária ${it + 1}/${DailyPool.DAILY_COUNT}</gray>" }
            ?: xp(current.mission.xp)
        return MissionCardText(
            "<white>${MissionText.requirement(objective, recipeName)}</white>",
            corner,
            "<aqua>${count(objective, current.progress)}</aqua>",
            fraction(objective, current.progress),
        )
    }

    fun completed(mission: Mission, xp: Int): MissionCardText =
        MissionCardText(
            "<green>Concluída:</green> <white>${mission.text}</white>",
            xp(xp),
            "<green>${count(mission.objective, mission.objective.goal)}</green>",
            1f,
        )

    private fun xp(amount: Int): String =
        if (amount > 0) "<gold>+${MissionText.number(amount.toLong())} XP</gold>" else ""

    private fun count(objective: MissionObjective, progress: Long): String {
        val goal = objective.goal.coerceAtLeast(1)
        val shown = progress.coerceIn(0, goal)
        return when (objective) {
            is MissionObjective.Sell -> "$${MissionText.number(shown)}/$${MissionText.number(goal)}"
            else -> "${MissionText.number(shown)}/${MissionText.number(goal)}"
        }
    }

    private fun fraction(objective: MissionObjective, progress: Long): Float {
        val goal = objective.goal.coerceAtLeast(1)
        return progress.coerceIn(0, goal).toFloat() / goal
    }
}
