package one_block.system.custom_items.block

object StateCodec {

    private const val ENTRY_DELIMITER = ";"
    private const val LEVEL_DELIMITER = ":"
    private const val FILTER_DELIMITER = ";"
    private const val STORAGE_DELIMITER = "\n"

    fun encodeUpgrades(upgrades: Map<String, Int>): String =
        upgrades.entries.joinToString(ENTRY_DELIMITER) { (id, level) -> "$id$LEVEL_DELIMITER$level" }

    fun decodeUpgrades(raw: String): MutableMap<String, Int> {
        val result = mutableMapOf<String, Int>()
        if (raw.isBlank()) return result

        raw.split(ENTRY_DELIMITER).forEach { entry ->
            if (entry.isBlank()) return@forEach
            val parts = entry.split(LEVEL_DELIMITER)
            if (parts.size != 2) return@forEach
            val level = parts[1].toIntOrNull() ?: return@forEach
            result[parts[0]] = level
        }
        return result
    }

    fun encodeFilter(filter: Set<String>): String = filter.joinToString(FILTER_DELIMITER)

    fun decodeFilter(raw: String): MutableSet<String> =
        raw.split(FILTER_DELIMITER).filter { it.isNotBlank() }.toMutableSet()

    fun encodeStorage(entries: List<String>): String = entries.joinToString(STORAGE_DELIMITER)

    fun decodeStorage(raw: String): MutableList<String> =
        raw.split(STORAGE_DELIMITER).filter { it.isNotBlank() }.toMutableList()
}
