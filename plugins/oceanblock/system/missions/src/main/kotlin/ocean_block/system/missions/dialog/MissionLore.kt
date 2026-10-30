package ocean_block.system.missions.dialog

import ocean_block.system.missions.catalog.DailyPool
import ocean_block.system.missions.catalog.MissionTrail
import ocean_block.system.missions.model.DailyMission
import ocean_block.system.missions.model.MissionObjective
import ocean_block.system.missions.model.PlayerMissions
import ocean_block.system.missions.rules.MissionRules
import ocean_block.system.missions.text.MissionText
import java.time.Duration
import java.time.LocalDateTime

object MissionLore {

    const val TRAIL = "trilha"
    const val NO_DAILIES = "As diárias chegam em instantes"

    private const val TRAIL_ICON = "nether_star"
    private const val XP_ICON = "experience_bottle"
    private val MONEY = Regex("\\$([0-9][0-9.]*)")

    fun summary(state: PlayerMissions): String =
        if (MissionRules.trailDone(state)) "TRILHA COMPLETA" else "TRILHA ${state.trailIndex}/${MissionTrail.size}"

    fun dailiesDone(state: PlayerMissions): String =
        "| ${state.dailies.count { it.done }}/${state.dailies.size} DIÁRIAS"

    fun trailCount(state: PlayerMissions): String = "${state.trailIndex.coerceAtMost(MissionTrail.size)}/${MissionTrail.size}"

    fun trailPercent(state: PlayerMissions): String = (state.trailIndex.coerceAtMost(MissionTrail.size) * 100 / MissionTrail.size).toString()

    fun trailStyle(state: PlayerMissions): String = if (MissionRules.trailDone(state)) "good" else "muted"

    fun notice(state: PlayerMissions): Map<String, String>? =
        if (state.dailies.isEmpty()) mapOf("texto" to NO_DAILIES, "estilo" to "muted") else null

    fun listed(state: PlayerMissions): List<DailyMission> =
        state.dailies

    fun legend(state: PlayerMissions): List<Map<String, String>> =
        if (listed(state).isEmpty()) emptyList() else listOf(mapOf("legenda" to "sim"))

    fun selection(state: PlayerMissions, chosen: String?): String {
        if (chosen == TRAIL) return TRAIL
        val dailies = listed(state)
        dailies.firstOrNull { it.id == chosen }?.let { return it.id }
        return dailies.firstOrNull { it.id == state.activeDaily && !it.done }?.id
            ?: dailies.firstOrNull { !it.done }?.id
            ?: TRAIL
    }

    fun row(daily: DailyMission, state: PlayerMissions, selected: String): Map<String, String> {
        val mission = DailyPool.byId(daily.id)
        val active = daily.id == state.activeDaily && !daily.done
        return mapOf(
            "icone" to icon(mission?.icon ?: "map"),
            "texto" to plain(mission?.name ?: daily.id),
            "xp" to "+${MissionText.number(daily.xp.toLong())} XP",
            "xp_estilo" to if (daily.done) "good" else "ouro",
            "barra" to if (daily.done || mission == null) "100" else percent(mission.objective, daily.progress),
            "contagem" to if (daily.done) "Feita" else mission?.let { count(it.objective, daily.progress) }.orEmpty(),
            "contagem_estilo" to when {
                daily.done -> "good"
                active -> "accent"
                else -> "muted"
            },
            "faixa" to when {
                daily.done -> "ui:faixa_verde"
                active -> "ui:faixa_azul"
                daily.id == selected -> "ui:faixa_laranja"
                else -> ""
            },
            "selecionada" to (daily.id == selected).toString(),
            "dica" to "Ver esta diária",
        )
    }

    fun detail(state: PlayerMissions, selected: String, recipeName: (String) -> String = { it }): Map<String, String> {
        if (selected == TRAIL) return trailDetail(state, recipeName)
        val index = state.dailies.indexOfFirst { it.id == selected }
        val daily = state.dailies.getOrNull(index) ?: return trailDetail(state, recipeName)
        val mission = DailyPool.byId(daily.id) ?: return trailDetail(state, recipeName)
        val active = daily.id == state.activeDaily && !daily.done
        val progress = if (daily.done) mission.objective.goal else daily.progress
        val (status, style) = when {
            daily.done -> "concluída" to "good"
            active -> "valendo agora" to "accent"
            daily.progress > 0 -> "pausada" to "destaque"
            else -> "disponível" to "destaque"
        }
        return mapOf(
            "icone" to icon(mission.icon),
            "titulo" to plain(mission.name),
            "subtitulo" to "Diária ${index + 1} de ${state.dailies.size} · $status",
            "subtitulo_estilo" to style,
            "requisito" to plain(MissionText.requirement(mission.objective, recipeName)),
            "progresso" to count(mission.objective, progress),
            "barra" to percent(mission.objective, progress),
            "recompensa" to "+${MissionText.number(daily.xp.toLong())} XP",
            "nota" to "Só a diária escolhida avança",
            "botao" to when {
                daily.done -> "Feita"
                active -> "Valendo agora"
                else -> "Escolher esta diária"
            },
            "bloqueado" to (daily.done || active).toString(),
            "dica" to "Só a diária escolhida avança; as outras guardam o progresso.",
        )
    }

    fun rewards(state: PlayerMissions, selected: String): List<Map<String, String>> {
        val daily = state.dailies.firstOrNull { it.id == selected && selected != TRAIL }
        val xp = daily?.xp ?: MissionTrail.at(state.trailIndex)?.xp ?: return emptyList()
        return listOf(
            mapOf(
                "icone" to icon(XP_ICON),
                "nome" to "XP de nível",
                "valor" to "+${MissionText.number(xp.toLong())}",
                "valor_estilo" to if (daily?.done == true) "good" else "ouro",
            ),
        )
    }

    fun resetIn(now: LocalDateTime): String = "Renovam em ${resetClock(now)}"

    fun resetClock(now: LocalDateTime): String {
        val minutes = Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMinutes()
        return "%02d:%02d".format(minutes / 60, minutes % 60)
    }

    fun icon(name: String): String = if (name.contains(':')) name else "item:$name"

    fun plain(text: String): String = MONEY.replace(text) { "${it.groupValues[1]} coins" }

    private fun trailDetail(state: PlayerMissions, recipeName: (String) -> String): Map<String, String> {
        val mission = MissionTrail.at(state.trailIndex)
        if (mission == null) {
            return mapOf(
                "icone" to icon(TRAIL_ICON),
                "titulo" to "Trilha completa",
                "subtitulo" to "${MissionTrail.size} de ${MissionTrail.size} missões",
                "subtitulo_estilo" to "good",
                "requisito" to "Você fez todas as missões da trilha",
                "progresso" to "${MissionTrail.size}/${MissionTrail.size}",
                "barra" to "100",
                "recompensa" to "Recebida",
                "nota" to "Agora valem as diárias",
                "botao" to "Concluída",
                "bloqueado" to "true",
                "dica" to "Trilha completa",
            )
        }
        return mapOf(
            "icone" to icon(mission.icon),
            "titulo" to plain(mission.name),
            "subtitulo" to "Missão ${state.trailIndex + 1} de ${MissionTrail.size}",
            "subtitulo_estilo" to "destaque",
            "requisito" to plain(MissionText.requirement(mission.objective, recipeName)),
            "progresso" to count(mission.objective, state.trailProgress),
            "barra" to percent(mission.objective, state.trailProgress),
            "recompensa" to "+${MissionText.number(mission.xp.toLong())} XP",
            "nota" to "A trilha avança sozinha",
            "botao" to "Em andamento",
            "bloqueado" to "true",
            "dica" to "A trilha segue em ordem; não precisa escolher.",
        )
    }

    internal fun count(objective: MissionObjective, progress: Long): String {
        val goal = objective.goal.coerceAtLeast(1)
        return "${MissionText.number(progress.coerceIn(0, goal))}/${MissionText.number(goal)}"
    }

    internal fun percent(objective: MissionObjective, progress: Long): String {
        val goal = objective.goal.coerceAtLeast(1)
        return (progress.coerceIn(0, goal) * 100 / goal).toString()
    }
}
