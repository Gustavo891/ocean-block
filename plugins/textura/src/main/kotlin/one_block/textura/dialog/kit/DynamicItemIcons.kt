package one_block.textura.dialog.kit

import java.util.concurrent.ConcurrentHashMap

object DynamicItemIcons {

    private const val VANILLA = "minecraft:"
    private val NAME = Regex("[a-z0-9_./-]+")
    private val items = ConcurrentHashMap.newKeySet<String>()

    fun register(materials: Collection<String>) {
        items += materials.map { it.trim().removePrefix(VANILLA).lowercase() }.filter(NAME::matches)
    }

    fun all(): Set<String> = items.toSet()
}
