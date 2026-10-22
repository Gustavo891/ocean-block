package ocean_block.system.research.screen

import ocean_block.system.research.model.IslandResearch
import ocean_block.system.research.model.ResearchCategory
import ocean_block.system.research.model.ResearchDefinition
import ocean_block.system.research.model.ResearchStatus
import ocean_block.system.research.rules.ResearchRules
import ocean_block.system.research.tree.ResearchTree

object ResearchEntries {

    const val CARDS_PER_PAGE = 4
    const val COST_SLOTS = 4
    private const val PLANKS = "planks"
    private const val PLANKS_ICON = "oak_planks"
    private const val BULLET = "∙ "
    private const val OPEN = "✔ Aberta agora!"
    private const val BROWSE = "Clique para ver!"

    fun categories(active: ResearchCategory): List<Map<String, String>> =
        ResearchCategory.entries.map { category ->
            mapOf(
                "nome" to category.displayName,
                "icone" to category.icon,
                "ativa" to (category == active).toString(),
                "acao" to if (category == active) OPEN else BROWSE,
            )
        }

    fun card(def: ResearchDefinition, state: IslandResearch, now: Long): Map<String, String> {
        val status = ResearchRules.status(def, state, now)
        val delivered = state.delivered[def.id].orEmpty()
        val available = status == ResearchStatus.Available
        return mapOf(
            "icone" to def.icon,
            "nome" to def.name,
            "descricao" to def.description,
            "custo_lista" to def.cost.joinToString("\n") { cost ->
                val have = if (status == ResearchStatus.Done) cost.amount else minOf(cost.amount, delivered[cost.key] ?: 0)
                "$BULLET$have/${cost.amount} ${cost.labelFor(cost.amount)}"
            },
            "estado" to label(status),
            "tempo" to time(def, status),
            "pode_entregar" to available.toString(),
            "bloqueado" to (!available).toString(),
            "acao" to action(status),
            "rotulo" to "Leva ${duration(def.durationMs)}",
            "botao" to button(status),
        ) + status(status) + costSlots(def, status, delivered)
    }

    fun costIcons(): List<String> =
        ResearchTree.all.flatMap { it.cost }.map { if (it.key == PLANKS) PLANKS_ICON else it.key }.distinct()

    private fun status(status: ResearchStatus): Map<String, String> {
        val (text, style) = when (status) {
            is ResearchStatus.Locked -> "Bloqueada" to "bad"
            ResearchStatus.Available -> "Entregue os itens" to "good"
            ResearchStatus.Queued -> "Na fila" to "muted"
            is ResearchStatus.Running -> "Pronta em ${duration(status.remainingMs)}" to "muted"
            ResearchStatus.Done -> "✔ Concluída" to "good"
        }
        return mapOf("status" to text, "status_estilo" to style)
    }

    private fun costSlots(def: ResearchDefinition, status: ResearchStatus, delivered: Map<String, Int>): Map<String, String> {
        val paid = status !is ResearchStatus.Locked && status != ResearchStatus.Available
        return (0 until COST_SLOTS).flatMap { index ->
            val cost = def.cost.getOrNull(index)
            val have = cost?.let { if (paid) it.amount else minOf(it.amount, delivered[it.key] ?: 0) } ?: 0
            val left = (cost?.amount ?: 0) - have
            listOf(
                "c${index}_item" to (cost?.let { if (it.key == PLANKS) PLANKS_ICON else it.key }.orEmpty()),
                "c${index}_qtd" to (cost?.let { (if (left > 0) left else it.amount).toString() }.orEmpty()),
                "c${index}_nome" to (cost?.let { it.labelFor(it.amount) }.orEmpty()),
                "c${index}_tem" to (cost?.let { "<gray>Entregue $have/${it.amount}" }.orEmpty()),
                "c${index}_falta" to (left > 0).toString(),
            )
        }.toMap()
    }

    fun queue(state: IslandResearch, now: Long): List<Map<String, String>> {
        val running = state.running.mapNotNull { run ->
            val def = ResearchTree.byId[run.id] ?: return@mapNotNull null
            mapOf("nome" to def.name, "estado" to "Pesquisando", "tempo" to "faltam ${duration((run.endsAt - now).coerceAtLeast(0))}")
        }
        val waiting = state.queue.mapNotNull { id ->
            val def = ResearchTree.byId[id] ?: return@mapNotNull null
            mapOf("nome" to def.name, "estado" to "Na fila", "tempo" to "leva ${duration(def.durationMs)}")
        }
        return running + waiting
    }

    fun slotsText(used: Int, total: Int): String = "Vagas: $used/$total"

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

    private fun label(status: ResearchStatus): String = when (status) {
        is ResearchStatus.Locked -> "Bloqueada"
        ResearchStatus.Available -> "Disponível"
        ResearchStatus.Queued -> "Na fila"
        is ResearchStatus.Running -> "Pesquisando"
        ResearchStatus.Done -> "Concluída"
    }

    private fun time(def: ResearchDefinition, status: ResearchStatus): String = when (status) {
        is ResearchStatus.Running -> "faltam ${duration(status.remainingMs)}"
        ResearchStatus.Done -> ""
        else -> "leva ${duration(def.durationMs)}"
    }

    private fun button(status: ResearchStatus): String = when (status) {
        is ResearchStatus.Locked -> "Bloqueada"
        ResearchStatus.Available -> "Entregar"
        ResearchStatus.Queued -> "Na fila"
        is ResearchStatus.Running -> "Pesquisando"
        ResearchStatus.Done -> "Concluída"
    }

    private fun action(status: ResearchStatus): String = when (status) {
        is ResearchStatus.Locked -> "⌀ Requer: " + status.missing.joinToString(", ") { ResearchTree.byId[it]?.name ?: it }
        ResearchStatus.Available -> "Entregar"
        ResearchStatus.Queued -> "Na fila"
        is ResearchStatus.Running -> "faltam ${duration(status.remainingMs)}"
        ResearchStatus.Done -> "✔ Concluída"
    }
}
