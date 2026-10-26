package one_block.textura.registry

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import org.bukkit.configuration.file.YamlConfiguration
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.util.logging.Logger
import javax.imageio.ImageIO

class GlyphRegistrySheetTest : StringSpec({

    val logger = Logger.getLogger("GlyphRegistrySheetTest")

    fun pasta(yml: String): File {
        val dir = Files.createTempDirectory("textura-glyphs").toFile()
        dir.deleteOnExit()
        ImageIO.write(BufferedImage(16, 48, BufferedImage.TYPE_INT_ARGB), "png", File(dir, "mouse_clicks.png"))
        File(dir, "icons.yml").writeText(yml)
        return dir
    }

    "folha valida expande em um glyph por id" {
        val dir = pasta(
            """
            mouse_clicks:
              caminho: mouse_clicks.png
              amount: 3
              ids: [mouse_left, mouse_right, mouse_middle]
              unicode:
                - \uE200
                - \uE201
                - \uE202
              ascent: 5
              height: 8
            """.trimIndent()
        )

        val registry = GlyphRegistry(logger)
        registry.load(dir)

        registry.glyphs.keys.toList() shouldContainExactly listOf("mouse_left", "mouse_right", "mouse_middle")

        val left = registry.glyphs.getValue("mouse_left")
        left.unicode shouldBe "\\uE200"
        left.sheetKey shouldBe "mouse_clicks"
        left.sheetIndex shouldBe 0
        left.ascent shouldBe 5
        left.height shouldBe 8
        left.caminho shouldBe "mouse_clicks.png"
        left.size shouldBe 16

        val middle = registry.glyphs.getValue("mouse_middle")
        middle.sheetIndex shouldBe 2
        middle.unicode shouldBe "\\uE202"
    }

    "folha sem ids e ignorada" {
        val dir = pasta(
            """
            mouse_clicks:
              caminho: mouse_clicks.png
              amount: 3
              unicode:
                - \uE200
                - \uE201
                - \uE202
            """.trimIndent()
        )

        val registry = GlyphRegistry(logger)
        registry.load(dir)

        registry.glyphs.size shouldBe 0
    }

    "folha com ids de tamanho diferente de amount e ignorada" {
        val dir = pasta(
            """
            mouse_clicks:
              caminho: mouse_clicks.png
              amount: 3
              ids: [mouse_left, mouse_right]
              unicode:
                - \uE200
                - \uE201
                - \uE202
            """.trimIndent()
        )

        val registry = GlyphRegistry(logger)
        registry.load(dir)

        registry.glyphs.size shouldBe 0
    }

    "amount zero e ignorado" {
        val dir = pasta(
            """
            mouse_clicks:
              caminho: mouse_clicks.png
              amount: 0
              ids: [mouse_left]
            """.trimIndent()
        )

        val registry = GlyphRegistry(logger)
        registry.load(dir)

        registry.glyphs.size shouldBe 0
    }

    "glyph simples continua carregando sem sheetKey" {
        val dir = pasta(
            """
            floating_islands:
              id: floating_islands
              caminho: mouse_clicks.png
              unicode: \uE103
              ascent: 8
              height: 8
            """.trimIndent()
        )

        val registry = GlyphRegistry(logger)
        registry.load(dir)

        registry.glyphs shouldContainKey "floating_islands"
        val glyph = registry.glyphs.getValue("floating_islands")
        glyph.sheetKey shouldBe null
        glyph.sheetIndex shouldBe 0
        glyph.unicode shouldBe "\\uE103"
    }

    "folha sem unicode gera um por fatia e persiste no yml" {
        val dir = pasta(
            """
            mouse_clicks:
              caminho: mouse_clicks.png
              amount: 3
              ids: [mouse_left, mouse_right, mouse_middle]
              ascent: 5
              height: 8
            """.trimIndent()
        )

        val registry = GlyphRegistry(logger)
        registry.load(dir)

        registry.glyphs.size shouldBe 3
        val gerados = listOf("mouse_left", "mouse_right", "mouse_middle")
            .map { registry.glyphs.getValue(it).unicode }
        gerados.toSet() shouldHaveSize 3
        gerados.all { it.startsWith("\\u") } shouldBe true

        val persistidos = YamlConfiguration
            .loadConfiguration(File(dir, "icons.yml"))
            .getStringList("mouse_clicks.unicode")
        persistidos shouldContainExactly gerados
    }

    "unicode gerado nao colide com unicode reservado de outro glyph" {
        val dir = pasta(
            """
            reservado:
              id: reservado
              caminho: mouse_clicks.png
              unicode: \uE000
            mouse_clicks:
              caminho: mouse_clicks.png
              amount: 2
              ids: [mouse_left, mouse_right]
            """.trimIndent()
        )

        val registry = GlyphRegistry(logger)
        registry.load(dir)

        val folha = listOf("mouse_left", "mouse_right").map { registry.glyphs.getValue(it).unicode }
        folha.contains("\\uE000") shouldBe false
        folha.toSet() shouldHaveSize 2
    }

    "unicode de folha reservado na primeira passada bloqueia glyph simples" {
        val dir = pasta(
            """
            mouse_clicks:
              caminho: mouse_clicks.png
              amount: 2
              ids: [mouse_left, mouse_right]
              unicode:
                - \uE000
                - \uE001
            tardio:
              id: tardio
              caminho: mouse_clicks.png
            """.trimIndent()
        )

        val registry = GlyphRegistry(logger)
        registry.load(dir)

        registry.glyphs.getValue("tardio").unicode shouldBe "\\uE002"
    }

    "glyph simples le a subida do texto e fica em 0 sem ela" {
        val dir = pasta(
            """
            linha:
              id: toast_linha
              caminho: mouse_clicks.png
              unicode: 
              ascent: 0
              height: 10
              subida: 2
            outro:
              id: toast_card
              caminho: mouse_clicks.png
              unicode: 
              ascent: 0
              height: 10
            """.trimIndent()
        )

        val registry = GlyphRegistry(logger)
        registry.load(dir)

        registry.glyphs.getValue("toast_linha").raise shouldBe 2
        registry.glyphs.getValue("toast_card").raise shouldBe 0
    }
})
