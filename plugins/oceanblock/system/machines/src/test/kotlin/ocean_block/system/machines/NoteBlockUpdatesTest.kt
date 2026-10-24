package ocean_block.system.machines

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.io.File

class NoteBlockUpdatesTest : StringSpec({

    fun projectRoot(): File? =
        generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").isFile }

    "paper nao atualiza note blocks, senao a maquina troca de aparencia quando o bloco vizinho muda" {
        val config = projectRoot().shouldNotBeNull().resolve("server/config/paper-global.yml")
        val line = config.readLines().map { it.trim() }.firstOrNull { it.startsWith("disable-noteblock-updates:") }
        line shouldBe "disable-noteblock-updates: true"
    }
})
