package ocean_block.system.missions.text

import ocean_block.system.missions.catalog.DailyPool
import ocean_block.system.missions.model.Mission
import ocean_block.system.missions.model.MissionObjective
import ocean_block.system.missions.rules.MissionRules
import java.util.Locale

object MissionText {

    private val locale: Locale = Locale.of("pt", "BR")

    fun hud(current: MissionRules.Current?, trailDone: Boolean): String {
        if (current == null) {
            return if (trailDone) "<gray>Diárias concluídas. Volte amanhã!" else ""
        }
        val label = current.dailyIndex?.let { "Diária ${it + 1}/${DailyPool.DAILY_COUNT}:" } ?: "Missão:"
        val counter = counter(current.mission.objective, current.progress)
        val tail = if (counter.isEmpty()) "" else " <gray>$counter"
        return "<aqua>$label</aqua> <white>${current.mission.text}</white>$tail"
    }

    fun completed(mission: Mission, xp: Int): String =
        "<green>Missão concluída:</green> <white>${mission.text}</white>" + if (xp > 0) " <gold>+$xp XP" else ""

    fun counter(objective: MissionObjective, progress: Long): String {
        val goal = objective.goal
        if (goal <= 1) return ""
        val shown = progress.coerceIn(0, goal)
        return when (objective) {
            is MissionObjective.Sell -> "$${number(shown)}/$${number(goal)}"
            else -> "${number(shown)}/${number(goal)}"
        }
    }

    fun number(value: Long): String = "%,d".format(locale, value)
}
