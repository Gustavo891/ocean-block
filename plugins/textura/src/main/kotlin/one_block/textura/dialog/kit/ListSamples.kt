package one_block.textura.dialog.kit

import one_block.textura.dialog.json.DefContainer
import one_block.textura.dialog.json.DefNode
import one_block.textura.dialog.json.DefRepeat
import one_block.textura.dialog.json.DefScoped
import one_block.textura.dialog.json.ScreenDefinition

object ListSamples {

    const val MAX_COMBINATIONS = 512
    private const val UNPAGED = 1

    fun limits(definition: ScreenDefinition, warn: (String) -> Unit): Map<String, Int> {
        val repeats = mutableListOf<DefRepeat>()
        definition.containers().forEach { collect(it, repeats::add) }
        return repeats.groupBy { it.list }.mapValues { (list, nodes) ->
            nodes.mapNotNull { it.perPage }.maxOrNull() ?: UNPAGED.also {
                warn("lista '$list' sem perPage: o gerar só pré-renderiza 0 e $UNPAGED entrada; alturas maiores ficam sem fundo")
            }
        }
    }

    fun combinations(limits: Map<String, Int>, warn: (String) -> Unit = {}): List<Map<String, Int>> {
        val total = limits.values.fold(1L) { acc, max -> if (acc > MAX_COMBINATIONS) acc else acc * (max + 1L) }
        if (total <= MAX_COMBINATIONS) {
            return limits.entries.fold(listOf(emptyMap())) { acc, (list, max) ->
                acc.flatMap { partial -> (0..max).map { count -> partial + (list to count) } }
            }
        }
        warn("as combinações de listas passam de $MAX_COMBINATIONS; o gerar varia uma lista por vez com as outras cheias")
        return (listOf(limits) + limits.flatMap { (list, max) -> (0..max).map { count -> limits + (list to count) } }).distinct()
    }

    private fun collect(node: DefNode, sink: (DefRepeat) -> Unit) {
        when (node) {
            is DefContainer -> node.children.forEach { collect(it, sink) }
            is DefRepeat -> {
                sink(node)
                collect(node.node, sink)
            }
            is DefScoped -> collect(node.node, sink)
            else -> Unit
        }
    }
}
