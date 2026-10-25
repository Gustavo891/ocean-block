package one_block.textura.hud

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor
import one_block.textura.api.TexturaTags
import one_block.textura.hud.anchor.AnchorMarker
import one_block.textura.hud.anchor.AnchorPalette
import one_block.textura.text.SpacingFont
import java.io.File

class HudRaiseTest : StringSpec({

    fun texts(component: Component): List<TextComponent> =
        listOfNotNull((component as? TextComponent)?.takeIf { it.content().isNotEmpty() }) +
            component.children().flatMap(::texts)

    "a marca de subir usa a ancora 7 e a cor da paleta" {
        AnchorMarker.raisedGlyph(AnchorPalette.AQUA) shouldBe TextColor.color(254, 112, 176)
        AnchorMarker.raisedShadow(AnchorPalette.DARK_GRAY, 30) shouldBe ShadowColor.shadowColor(253, 112, 128, 30)
    }

    "todo texto do HUD sai marcado para subir, cada um na sua cor" {
        val raised = HudRaise.mark(TexturaTags.miniMessage.deserialize("<aqua>Missão:</aqua> <white>Colete</white> <gray>3/10"))
        val parts = texts(raised)

        parts.shouldNotBeEmpty()
        parts.forEach { part ->
            val color = part.color().shouldNotBeNull()
            color.red() shouldBe AnchorMarker.GLYPH_RED
            color.green() shouldBe AnchorMarker.RAISE_ID * AnchorMarker.STEP
            part.shadowColor().shouldNotBeNull().red() shouldBe AnchorMarker.SHADOW_RED
        }
        parts.first { it.content() == "Missão:" }.color() shouldBe AnchorMarker.raisedGlyph(AnchorPalette.AQUA)
        parts.first { it.content() == "3/10" }.color() shouldBe AnchorMarker.raisedGlyph(AnchorPalette.GRAY)
    }

    "a placa do fundo sai marcada para subir junto com o texto" {
        val plate = HudBackground.build(3, HudBackground.Glyphs("a", "b", "c", 6, 6, 6))
        val parts = texts(plate).filter { it.font() != SpacingFont.KEY }

        parts.map { it.content() } shouldBe listOf("a", "b", "c")
        parts.forEach { part ->
            part.color() shouldBe AnchorMarker.raisedGlyph(AnchorPalette.WHITE)
            part.shadowColor() shouldBe AnchorMarker.raisedShadow(AnchorPalette.GRAY, 30)
        }
    }

    "o shader sobe a ancora 7 uma bossbar" {
        val shader = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").isFile }
            .shouldNotBeNull()
            .resolve("server/plugins/Textura/resource/assets/minecraft/shaders/core/text.vsh")
            .readText()

        shader shouldContain "const int TEXTURA_HUD_ANCHORS = 7;"
        shader shouldContain "const float TEXTURA_HUD_BAR_STEP = 19.0;"
        shader shouldContain "if (anchor == 7) return vec2(0.0, -TEXTURA_HUD_BAR_STEP);"
    }
})
