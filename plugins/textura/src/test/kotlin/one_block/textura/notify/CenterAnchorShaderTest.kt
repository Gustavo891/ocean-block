package one_block.textura.notify

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import java.io.File

class CenterAnchorShaderTest : StringSpec({

    "o shader centraliza a ancora 8 e anima pelo lift" {
        val shader = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").isFile }
            .shouldNotBeNull()
            .resolve("server/plugins/Textura/resource/assets/minecraft/shaders/core/text.vsh")
            .readText()

        shader shouldContain "const int TEXTURA_HUD_ANCHORS = 8;"
        shader shouldContain "const float TEXTURA_HUD_CENTER_TOP = 32.0;"
        shader shouldContain "if (anchor == 8) return vec2(0.0, floor(guiHeight * 0.5) - TEXTURA_HUD_BAR_TEXT_TOP - TEXTURA_HUD_CENTER_TOP);"
        shader shouldContain "if (hudAnchor == 8) {"
    }
})
