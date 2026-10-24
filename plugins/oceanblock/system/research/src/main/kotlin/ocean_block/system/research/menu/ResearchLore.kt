package ocean_block.system.research.menu

import ocean_block.system.research.model.IslandResearch
import ocean_block.system.research.model.ResearchCategory
import ocean_block.system.research.model.ResearchDefinition
import ocean_block.system.research.model.ResearchStatus
import ocean_block.system.research.rules.ResearchRules
import ocean_block.system.research.tree.ResearchTree

object ResearchLore {

    const val CATEGORY_SLOTS = 4

    fun name(def: ResearchDefinition, state: IslandResearch, now: Long): String {
        val color = if (ResearchRules.status(def, state, now) == ResearchStatus.Done) "<green>" else "<white>"
        return color + def.name
    }

    fun lore(def: ResearchDefinition, state: IslandResearch, now: Long): List<String> {
        val status = ResearchRules.status(def, state, now)
        val paid = status !is ResearchStatus.Locked && status != ResearchStatus.Available
        val delivered = state.delivered[def.id].orEmpty()
        val costs = def.cost.map { cost ->
            val have = if (paid) cost.amount else minOf(cost.amount, delivered[cost.key] ?: 0)
            val mark = if (have < cost.amount) "<red>✖" else "<green>✔"
            "$mark <gray>$have/${cost.amount} ${cost.labelFor(cost.amount)}"
        }
        return listOf("<gray>${def.description}", "", "<white>Custo:") + costs +
            listOf("", "<gray>Duração: <white>${duration(def.durationMs)}", "", status(status))
    }

    fun categoryName(category: ResearchCategory, active: Boolean): String =
        (if (active) "<green>" else "<white>") + category.displayName

    fun categoryLore(active: Boolean): List<String> =
        listOf(if (active) "<green>Aberta agora." else "<yellow>Clique para ver!")

    fun slotsName(used: Int, total: Int): String = "<white>Vagas: $used/$total"

    fun queueLore(state: IslandResearch, now: Long): List<String> {
        val running = state.running.mapNotNull { run ->
            ResearchTree.byId[run.id]?.let { "<aqua>${it.name} <gray>faltam ${duration((run.endsAt - now).coerceAtLeast(0))}" }
        }
        val waiting = state.queue.mapNotNull { id ->
            ResearchTree.byId[id]?.let { "<gold>${it.name} <gray>na fila, leva ${duration(it.durationMs)}" }
        }
        return (running + waiting).ifEmpty { listOf("<gray>Nenhuma pesquisa em andamento.") }
    }

    fun duration(ms: Long): String {
        val seconds = ((ms + 999) / 1000).coerceAtLeast(0)
        val minutes = seconds / 60
        val rest = seconds % 60
        return when {
            minutes == 0L -> "${rest}s"
            rest == 0L -> "${minutes}min"
            else -> "${minutes}m${rest}s"
        }
    }

    private fun status(status: ResearchStatus): String = when (status) {
        is ResearchStatus.Locked -> "<red>Requer: " + status.missing.joinToString(", ") { ResearchTree.byId[it]?.name ?: it }
        ResearchStatus.Available -> "<yellow>Clique para entregar os itens!"
        ResearchStatus.Queued -> "<gold>Na fila."
        is ResearchStatus.Running -> "<aqua>Pesquisando: faltam ${duration(status.remainingMs)}"
        ResearchStatus.Done -> "<green>✔ Concluída"
    }
}
