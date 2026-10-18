package one_block.textura.registry

import one_block.textura.dialog.json.ScreenDefinition
import one_block.textura.dialog.json.ScreenParser
import java.io.File
import java.util.logging.Logger

class UiScreenRegistry(private val logger: Logger) {

    @Volatile
    var screens: Map<String, ScreenDefinition> = emptyMap()
        private set

    fun load(folder: File) {
        val parser = ScreenParser(logger)
        screens = folder.listFiles { file -> file.isFile && file.extension == "json" }.orEmpty()
            .sortedBy { it.name }
            .mapNotNull(parser::parse)
            .associateBy { it.id }
    }
}
