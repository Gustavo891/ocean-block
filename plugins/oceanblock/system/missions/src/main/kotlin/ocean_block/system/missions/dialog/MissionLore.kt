package ocean_block.system.missions.dialog

import ocean_block.system.missions.catalog.DailyPool
import ocean_block.system.missions.catalog.MissionTrail
import ocean_block.system.missions.model.DailyMission
import ocean_block.system.missions.model.Mission
import ocean_block.system.missions.model.MissionObjective
import ocean_block.system.missions.model.PlayerMissions
import ocean_block.system.missions.rules.MissionRules
import ocean_block.system.missions.text.MissionText
import java.time.Duration
import java.time.LocalDateTime

object MissionLore {

    const val LOCKED_DAILIES = "Termine a trilha para liberar as diárias"
    const val CHOOSE_DAILY = "Escolha uma diária abaixo"
    const val DAILIES_DONE = "Diárias concluídas. Voltam amanhã!"

    private val MONEY = Regex("\\$([0-9][0-9.]*)")

    fun summary(state: PlayerMissions): String =
        if (MissionRules.trailDone(state)) "TRILHA COMPLETA" else "TRILHA ${state.trailIndex}/${MissionTrail.size}"

    fun active(state: PlayerMissions, recipeName: (String) -> String = { it }): Map<String, String>? {
        MissionTrail.at(state.trailIndex)?.let { mission ->
            return card("Missão ${state.trailIndex + 1} de ${MissionTrail.size}", mission, state.trailProgress, mission.xp, recipeName)
        }
        val current = MissionRules.current(state) ?: return null
        val xp = current.dailyIndex?.let { state.dailies.getOrNull(it)?.xp } ?: current.mission.xp
        return card("Valendo agora", current.mission, current.progress, xp, recipeName)
    }

    fun notice(state: PlayerMissions): Map<String, String>? = when {
        !MissionRules.trailDone(state) -> mapOf("texto" to LOCKED_DAILIES, "estilo" to "muted")
        MissionRules.needsChoice(state) -> mapOf("texto" to CHOOSE_DAILY, "estilo" to "accent")
        state.dailies.isNotEmpty() && state.dailies.all { it.done } -> mapOf("texto" to DAILIES_DONE, "estilo" to "good")
        else -> null
    }

    fun listed(state: PlayerMissions): List<DailyMission> =
        if (!MissionRules.trailDone(state)) emptyList()
        else state.dailies.filter { it.done || it.id != state.activeDaily }

    fun row(daily: DailyMission, recipeName: (String) -> String = { it }): Map<String, String> {
        val mission = DailyPool.byId(daily.id)
        val count = mission?.let { count(it.objective, if (daily.done) it.objective.goal else daily.progress) }.orEmpty()
        val requirement = mission?.let { plain(MissionText.requirement(it.objective, recipeName)) }.orEmpty()
        return mapOf(
            "texto" to plain(mission?.text ?: daily.id),
            "icone" to icon(mission?.icon ?: "map"),
            "xp" to "+${MissionText.number(daily.xp.toLong())} XP",
            "situacao" to when {
                daily.done -> "Concluída"
                daily.progress > 0 -> "$count  Pausada"
                else -> count
            },
            "situacao_estilo" to if (daily.done) "good" else "muted",
            "acao" to if (daily.done) "Feita" else "Escolher",
            "bloqueado" to daily.done.toString(),
            "dica" to if (daily.done) "Diária concluída"
            else "Escolher esta diária\n\n$requirement\n\nSó a diária escolhida avança; as outras guardam o progresso.",
        )
    }

    fun resetIn(now: LocalDateTime): String {
        val minutes = Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMinutes()
        return "Renovam em %02d:%02d".format(minutes / 60, minutes % 60)
    }

    fun icon(name: String): String = if (name.contains(':')) name else "item:$name"

    fun plain(text: String): String = MONEY.replace(text) { "${it.groupValues[1]} coins" }

    private fun card(label: String, mission: Mission, progress: Long, xp: Int, recipeName: (String) -> String): Map<String, String> {
        val text = plain(mission.text)
        val requirement = plain(MissionText.requirement(mission.objective, recipeName))
        return mapOf(
            "rotulo" to label,
            "texto" to text,
            "icone" to icon(mission.icon),
            "requisito" to if (requirement.equals(text, ignoreCase = true)) "" else requirement,
            "xp" to "+${MissionText.number(xp.toLong())} XP",
            "barra" to percent(mission.objective, progress),
            "situacao" to count(mission.objective, progress),
        )
    }

    private fun count(objective: MissionObjective, progress: Long): String {
        val goal = objective.goal.coerceAtLeast(1)
        return "${MissionText.number(progress.coerceIn(0, goal))}/${MissionText.number(goal)}"
    }

    private fun percent(objective: MissionObjective, progress: Long): String {
        val goal = objective.goal.coerceAtLeast(1)
        return (progress.coerceIn(0, goal) * 100 / goal).toString()
    }
}
