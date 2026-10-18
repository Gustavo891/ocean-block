package one_block.textura.registry

import java.io.File

object YamlFiles {

    fun collect(root: File): List<File> {
        if (!root.isDirectory) return emptyList()
        return root.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".yml") }
            .sortedBy { it.relativeTo(root).invariantSeparatorsPath }
            .toList()
    }
}
