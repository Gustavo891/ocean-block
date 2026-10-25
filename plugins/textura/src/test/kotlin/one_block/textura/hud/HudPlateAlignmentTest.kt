package one_block.textura.hud

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class HudPlateAlignmentTest : StringSpec({

    fun texturaConfigs(): File =
        generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").isFile }
            .shouldNotBeNull()
            .resolve("server/plugins/Textura/configs")

    "a placa do HUD fica centrada no texto da fonte bossbar" {
        val glyphs = YamlConfiguration.loadConfiguration(texturaConfigs().resolve("glyphs/icons.yml"))
        val fonts = YamlConfiguration.loadConfiguration(texturaConfigs().resolve("fonts/fonts.yml"))
        val textTop = fonts.getInt("bossbar.ascent")
        val textBottom = textTop - 8

        listOf("hud_left", "hud_body", "hud_right").forEach { id ->
            val plateTop = glyphs.getInt("$id.ascent")
            val plateBottom = plateTop - glyphs.getInt("$id.height")
            val above = plateTop - textTop
            val below = textBottom - plateBottom

            above shouldBeGreaterThanOrEqual 1
            above shouldBe below
        }
    }
})
