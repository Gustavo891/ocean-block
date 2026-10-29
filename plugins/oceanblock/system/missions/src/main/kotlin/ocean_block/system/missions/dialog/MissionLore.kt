package ocean_block.system.missions.dialog

import ocean_block.system.missions.catalog.DailyPool
import ocean_block.system.missions.catalog.MissionTrail
import ocean_block.system.missions.model.DailyMission
import ocean_block.system.missions.model.MissionObjective
import ocean_block.system.missions.model.PlayerMissions
import ocean_block.system.missions.text.MissionText
import java.time.Duration
import java.time.LocalDateTime

object MissionLore {

    const val LOCKED_DAILIES = "Termine a trilha para liberar as diárias."

    fun trail(state: PlayerMissions, recipeName: (String) -> String = { it }): Map<String, String> {
        val mission = MissionTrail.at(state.trailIndex) ?: return mapOf(
            "titulo" to "Trilha concluída!",
            "texto" to "Você completou as ${MissionTrail.size} missões da trilha.",
            "contador" to "",
            "xp" to "",
            "barra" to "100",
            "icone" to "item:nether_star",
            "requisito" to "",
            "recompensa" to "",
        )
        return mapOf(
            "icone" to "item:${mission.icon}",
            "requisito" to MissionText.requirement(mission.objective, recipeName),
            "recompensa" to MissionText.reward(mission.xp),
            "titulo" to "Missão ${state.trailIndex + 1} de ${MissionTrail.size}",
            "texto" to mission.text,
            "contador" to MissionText.counter(mission.objective, state.trailProgress),
            "xp" to "+${MissionText.number(mission.xp.toLong())} XP",
            "barra" to percent(mission.objective, state.trailProgress),
        )
    }

    fun daily(daily: DailyMission, active: Boolean = false, recipeName: (String) -> String = { it }): Map<String, String> {
        val mission = DailyPool.byId(daily.id)
        val progress = if (daily.done) mission?.objective?.goal ?: 1 else daily.progress
        return mapOf(
            "texto" to (mission?.text ?: daily.id),
            "icone" to "item:${mission?.icon ?: "map"}",
            "requisito" to (mission?.let { MissionText.requirement(it.objective, recipeName) } ?: ""),
            "recompensa" to MissionText.reward(daily.xp),
            "contador" to (mission?.let { MissionText.counter(it.objective, progress) } ?: ""),
            "xp" to "+${MissionText.number(daily.xp.toLong())} XP",
            "barra" to if (daily.done || mission == null) "100" else percent(mission.objective, progress),
            "feito" to daily.done.toString(),
            "estado" to if (daily.done) "Concluída" else "Em andamento",
            "estado_estilo" to if (daily.done) "good" else "muted",
            "situacao" to if (daily.done) "Concluída" else (mission?.let { MissionText.counter(it.objective, progress) } ?: ""),
            "situacao_estilo" to if (daily.done) "good" else "muted",
            "acao" to when {
                daily.done -> "Feita"
                active -> "Ativa"
                else -> "Escolher"
            },
            "bloqueado" to (daily.done || active).toString(),
            "dica" to when {
                daily.done -> "Diária concluída"
                active -> "Esta é a diária que está valendo agora"
                else -> "Escolher esta diária\n\nSó a diária escolhida avança; as outras guardam o progresso."
            },
        )
    }

    fun resetIn(now: LocalDateTime): String {
        val minutes = Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMinutes()
        return "Renovam em %02d:%02d".format(minutes / 60, minutes % 60)
    }

    private fun percent(objective: MissionObjective, progress: Long): String =
        (progress.coerceIn(0, objective.goal) * 100 / objective.goal).toString()
}
