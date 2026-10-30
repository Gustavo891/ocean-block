package ocean_block.system.missions.dialog

import ocean_block.system.missions.catalog.MissionTrail
import ocean_block.system.missions.model.PlayerMissions
import ocean_block.system.missions.rules.MissionRules
import ocean_block.system.missions.text.MissionText

object TrailLore {

    const val PER_ROW = 5
    const val ROWS = 3

    private const val XP_ICON = "experience_bottle"

    fun order(): List<List<Int>> = (0 until ROWS).map { row ->
        val indices = (row * PER_ROW until (row + 1) * PER_ROW).toList()
        if (row % 2 == 1) indices.reversed() else indices
    }

    fun selection(state: PlayerMissions, chosen: Int?): Int =
        chosen?.takeIf { it in MissionTrail.all.indices } ?: state.trailIndex.coerceAtMost(MissionTrail.size - 1)

    fun row(state: PlayerMissions, row: Int, selected: Int): List<Map<String, String>> {
        val indices = order().getOrNull(row) ?: return emptyList()
        return indices.mapIndexed { position, index ->
            val next = indices.getOrNull(position + 1)
            node(state, index, selected) + ("liga" to (next?.let { link(state, maxOf(index, it)) } ?: ""))
        }
    }

    fun drop(state: PlayerMissions, row: Int): Map<String, String> {
        val reached = (row + 1) * PER_ROW
        return mapOf("liga" to if (reached <= state.trailIndex) "ui:desce_verde" else "ui:desce_cinza")
    }

    fun indexAt(row: Int, position: Int): Int? = order().getOrNull(row)?.getOrNull(position)

    fun summary(state: PlayerMissions): String =
        if (MissionRules.trailDone(state)) "TRILHA COMPLETA" else "${state.trailIndex}/${MissionTrail.size} FEITAS"

    fun xp(state: PlayerMissions): String {
        val earned = MissionTrail.all.take(state.trailIndex).sumOf { it.xp }.toLong()
        val total = MissionTrail.all.sumOf { it.xp }.toLong()
        return "| ${MissionText.number(earned)} DE ${MissionText.number(total)} XP"
    }

    fun dots(state: PlayerMissions): List<Map<String, String>> =
        state.dailies.map { daily ->
            mapOf(
                "ponto" to when {
                    daily.done -> "ui:ponto_feito"
                    daily.id == state.activeDaily -> "ui:ponto_ativo"
                    else -> "ui:ponto_vazio"
                },
            )
        }

    fun detail(state: PlayerMissions, index: Int, recipeName: (String) -> String = { it }): Map<String, String> {
        val mission = MissionTrail.at(index) ?: return emptyMap()
        val goal = mission.objective.goal
        val progress = when {
            index < state.trailIndex -> goal
            index == state.trailIndex -> state.trailProgress
            else -> 0
        }
        return mapOf(
            "icone" to MissionLore.icon(mission.icon),
            "titulo" to MissionLore.plain(mission.name),
            "requisito" to MissionLore.plain(MissionText.requirement(mission.objective, recipeName)),
            "barra" to MissionLore.percent(mission.objective, progress),
            "progresso" to MissionLore.count(mission.objective, progress),
            "nota" to "Missão ${index + 1} de ${MissionTrail.size}",
            "botao" to when {
                index < state.trailIndex -> "Feita"
                index == state.trailIndex -> "Em andamento"
                else -> "Trancada"
            },
            "bloqueado" to "true",
            "dica" to "A trilha segue em ordem; não precisa escolher.",
        )
    }

    fun rewards(index: Int): List<Map<String, String>> {
        val mission = MissionTrail.at(index) ?: return emptyList()
        return listOf(
            mapOf(
                "icone" to MissionLore.icon(XP_ICON),
                "nome" to "XP de nível",
                "valor" to "+${MissionText.number(mission.xp.toLong())}",
                "valor_estilo" to "ouro",
            ),
        )
    }

    private fun node(state: PlayerMissions, index: Int, selected: Int): Map<String, String> {
        val mission = MissionTrail.all[index]
        return mapOf(
            "icone" to MissionLore.icon(mission.icon),
            "estado" to when {
                index < state.trailIndex -> ""
                index == state.trailIndex -> "ui:no_atual"
                else -> "ui:no_trancada"
            },
            "selo" to if (index < state.trailIndex) "ui:no_feita" else "",
            "selecionada" to (index == selected && index != state.trailIndex).toString(),
            "dica" to "${index + 1}. ${mission.name}",
        )
    }

    private fun link(state: PlayerMissions, later: Int): String =
        if (later <= state.trailIndex) "ui:liga_verde" else "ui:liga_cinza"
}
