package one_block.textura.notify.icon

import java.util.concurrent.ConcurrentHashMap

object ItemIconRegistry {

    private val NAME = Regex("[a-z0-9_]+")
    private val materials = ConcurrentHashMap.newKeySet<String>()

    fun register(names: Collection<String>): List<String> {
        val invalid = mutableListOf<String>()
        names.forEach { raw ->
            val name = raw.trim().lowercase().removePrefix("minecraft:")
            if (NAME.matches(name)) materials += name else invalid += raw
        }
        return invalid
    }

    fun materials(): Set<String> = materials.toSortedSet()
}
