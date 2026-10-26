package one_block.textura.notify.region

import one_block.textura.notify.NotificationLine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import one_block.textura.hud.anchor.AnchorMarker
import one_block.textura.hud.anchor.AnchorPalette
import one_block.textura.hud.anchor.ScreenAnchor
import one_block.textura.notify.region.NotificationRegion
import one_block.textura.notify.region.RegionConfigParser
import one_block.textura.notify.region.StackDirection
import one_block.textura.text.SpacingFont
import one_block.textura.text.TextMetrics
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.function.Consumer

class MarkerAndParserTest : StringSpec({

    fun parse(yml: String): Pair<List<NotificationRegion>, List<String>> {
        val warnings = mutableListOf<String>()
        val regions = RegionConfigParser.parse(YamlConfiguration().apply { loadFromString(yml) }, Consumer { warnings += it })
        return regions to warnings
    }

    "marcador põe âncora no G, paleta no nibble alto do B e lift no baixo" {
        val color = AnchorMarker.glyph(ScreenAnchor.BOTTOM_RIGHT, AnchorPalette.YELLOW, 5)
        color.red() shouldBe 254
        color.green() shouldBe 6 * 16
        color.blue() shouldBe 14 * 16 + 5

        val shadow = AnchorMarker.shadow(ScreenAnchor.BOTTOM_RIGHT, AnchorPalette.YELLOW, 5)
        shadow.red() shouldBe 253
        shadow.green() shouldBe 6 * 16
        shadow.blue() shouldBe 14 * 16 + 5
        shadow.alpha() shouldBe 255
    }

    "sem lift o marcador é o mesmo do spike" {
        AnchorMarker.glyph(ScreenAnchor.TOP_LEFT, AnchorPalette.WHITE) shouldBe AnchorMarker.glyph(ScreenAnchor.TOP_LEFT, AnchorPalette.WHITE, 0)
        AnchorMarker.glyph(ScreenAnchor.TOP_LEFT, AnchorPalette.WHITE).blue() shouldBe 240
    }

    "lift fora do nibble é recusado" {
        shouldThrow<IllegalArgumentException> { AnchorMarker.glyph(ScreenAnchor.TOP_LEFT, AnchorPalette.WHITE, 16) }
    }

    "cor do texto vira a entrada mais próxima da paleta" {
        AnchorPalette.nearest(null) shouldBe AnchorPalette.WHITE
        AnchorPalette.nearest(NamedTextColor.GREEN) shouldBe AnchorPalette.GREEN
        AnchorPalette.nearest(TextColor.color(0xFE1111)) shouldBe AnchorPalette.RED
        AnchorPalette.entries.map { it.index } shouldBe (0..15).toList()
    }

    "o notificacoes.yml versionado é exatamente o padrão" {
        val data = File(File(System.getProperty("user.dir")).parentFile.parentFile, "server/plugins/Textura/configs/hud/notificacoes.yml")
        val warnings = mutableListOf<String>()
        RegionConfigParser.load(data, Consumer { warnings += it }) shouldBe RegionConfigParser.DEFAULTS
        warnings.shouldBeEmpty()
    }

    "padrões do spec" {
        val (coleta, avisos) = RegionConfigParser.DEFAULTS
        coleta shouldBe NotificationRegion("coleta", ScreenAnchor.MIDDLE_RIGHT, 0, 70, 6, 1, true, StackDirection.DOWN)
        avisos shouldBe NotificationRegion("avisos", ScreenAnchor.TOP_LEFT, 2, 5, 5, 1, true, StackDirection.DOWN)
    }

    "arquivo ausente usa os padrões e avisa" {
        val warnings = mutableListOf<String>()
        RegionConfigParser.load(File("nao-existe/notificacoes.yml"), Consumer { warnings += it }) shouldBe RegionConfigParser.DEFAULTS
        warnings shouldHaveSize 1
    }

    "campos ausentes usam os padrões e base empilha para cima" {
        val (regions, warnings) = parse(
            """
            pe:
              ancora: base_direita
            """.trimIndent(),
        )
        warnings.shouldBeEmpty()
        regions.single() shouldBe NotificationRegion("pe", ScreenAnchor.BOTTOM_RIGHT, 0, 0, 3, 1, true, StackDirection.UP)
    }

    "âncora inválida descarta a região com aviso" {
        val (regions, warnings) = parse(
            """
            a:
              ancora: MEIO
            b:
              ancora: TOPO_DIREITA
            """.trimIndent(),
        )
        regions.map { it.id }.shouldContainExactly("b")
        warnings.single() shouldContain "ancora"
    }

    "valores inválidos avisam e caem no padrão ou no limite" {
        val (regions, warnings) = parse(
            """
            a:
              ancora: MEIO_ESQUERDA
              x: abc
              y: 2.5
              linhas: 40
              espacamento: -3
              fundo: talvez
              direcao: lado
            """.trimIndent(),
        )
        regions.single() shouldBe NotificationRegion("a", ScreenAnchor.MIDDLE_LEFT, 0, 0, 15, -2, true, StackDirection.DOWN)
        warnings shouldHaveSize 6
    }

    "id repetido mantém o primeiro" {
        val (regions, warnings) = parse(
            """
            a:
              id: x
              ancora: TOPO_ESQUERDA
            b:
              id: x
              ancora: TOPO_DIREITA
            """.trimIndent(),
        )
        regions.single().anchor shouldBe ScreenAnchor.TOP_LEFT
        warnings.single() shouldContain "repetida"
    }

    "linhas que passam do alcance do shader são cortadas com aviso" {
        val (regions, warnings) = parse(
            """
            a:
              ancora: BASE_ESQUERDA
              y: -150
              linhas: 10
            """.trimIndent(),
        )
        regions.single().maxLines shouldBe 3
        warnings.single() shouldContain "linhas"
    }

    "TextMetrics conta glyphs de shift pelo avanço real" {
        TextMetrics.calculateWidth("a" + SpacingFont.chars(-3) + "b") shouldBe 9
        TextMetrics.calculateWidth("a b") shouldBe 16
    }
})
