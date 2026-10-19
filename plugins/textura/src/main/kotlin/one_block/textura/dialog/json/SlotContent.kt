package one_block.textura.dialog.json

data class SlotView(val at: Int, val item: String, val count: Int, val name: String?, val lore: List<String>)

object SlotContent {

    private const val VANILLA = "minecraft:"
    const val MAX_COUNT = 99
    private val INVALID = Regex("""[\s{}]""")

    fun resolve(item: DefSlotItem, expand: (String) -> String): SlotView? {
        val material = expand(item.item).trim().removePrefix(VANILLA).lowercase()
        if (material.isEmpty() || INVALID.containsMatchIn(material)) return null
        val count = expand(item.count).trim().toIntOrNull()?.coerceIn(1, MAX_COUNT) ?: 1
        return SlotView(item.at, material, count, item.name, item.lore)
    }

    fun dynamic(item: DefSlotItem): Boolean = item.item.contains('{')
}
