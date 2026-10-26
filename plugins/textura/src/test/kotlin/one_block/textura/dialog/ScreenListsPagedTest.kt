package one_block.textura.dialog

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import one_block.textura.dialog.json.ScreenParser
import one_block.textura.dialog.kit.ListSamples
import java.io.File
import java.util.logging.Logger

class ScreenListsPagedTest : StringSpec({

    "toda lista das telas do servidor tem perPage, senão o fundo some com mais de uma entrada" {
        val ui = File(File(System.getProperty("user.dir")).parentFile.parentFile, "server/plugins/Textura/configs/ui")
        val logger = Logger.getAnonymousLogger().apply { useParentHandlers = false }
        val problems = ui.listFiles { file -> file.extension == "json" }.orEmpty().sortedBy { it.name }.flatMap { file ->
            val definition = ScreenParser(logger).parse(file) ?: return@flatMap listOf("${file.name}: ilegível")
            val warnings = mutableListOf<String>()
            ListSamples.limits(definition) { warnings += "${file.name}: $it" }
            warnings
        }

        problems.shouldBeEmpty()
    }
})
