package one_block.textura.dialog.json

class ListExpander(
    private val lists: (String) -> List<Map<String, String>>,
    private val page: (String) -> Int,
) {

    private val cache = mutableMapOf<String, List<Map<String, String>>>()

    fun expand(container: DefContainer): DefContainer = container(container, emptyMap())

    private fun container(def: DefContainer, scope: Map<String, String>): DefContainer =
        def.copy(children = def.children.flatMap { child(it, scope) })

    private fun child(def: DefNode, scope: Map<String, String>): List<DefNode> = when (def) {
        is DefRepeat -> window(def).map { (index, entry) ->
            val values = scope + entry
            DefScoped(values, def.list, index, inner(def.node, values), "${def.path}[$index]")
        }
        is DefContainer -> listOf(container(def, scope))
        else -> listOf(def)
    }

    private fun inner(def: DefNode, scope: Map<String, String>): DefNode =
        if (def is DefContainer) container(def, scope) else def

    private fun window(def: DefRepeat): List<IndexedValue<Map<String, String>>> {
        val entries = cache.getOrPut(def.list) { lists(def.list) }.withIndex().toList()
        val perPage = def.perPage ?: return entries
        if (entries.isEmpty()) return emptyList()
        val current = page(def.list).coerceIn(0, entries.lastIndex / perPage)
        return entries.drop(current * perPage).take(perPage)
    }
}
