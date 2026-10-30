package one_block.textura.pack

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import one_block.textura.dialog.json.ScreenParser
import one_block.textura.dialog.kit.UiArtCatalog
import one_block.textura.dialog.kit.UiTextFontLoader
import one_block.textura.registry.UiThemeRegistry
import team.unnamed.creative.font.BitMapFontProvider
import java.io.ByteArrayInputStream
import java.io.File
import java.util.logging.Logger
import javax.imageio.ImageIO

class UiPackRealScreensTest : StringSpec({

    "o pack de UI das telas reais passa nas regras do cliente" {
        val data = File(File(System.getProperty("user.dir")).parentFile.parentFile, "server/plugins/Textura")
        val logger = Logger.getAnonymousLogger().apply { useParentHandlers = false }
        val ui = File(data, "configs/ui")
        val fonts = File(data, "configs/fonts")
        val resource = File(data, "resource")
        val themes = UiThemeRegistry(logger).also { it.load(ui) }.themes
        val screens = ui.listFiles { file -> file.extension == "json" }.orEmpty().mapNotNull { ScreenParser(logger).parse(it) }
        val extras = themes.values.flatMap { theme -> theme.styles.values.mapNotNull { it.font } }.toSortedSet()
            .associateWith { UiTextFontLoader.load(File(fonts, "$it.json"), resource, logger) }
        val textFont = UiTextFontLoader.load(File(fonts, "custom.json"), resource, logger).withExtras(extras)
        val catalog = UiArtCatalog(File(resource, "assets/oneblock/textures/ui"), { null }, logger)
        val builder = PackBuilder()

        UiPackWriter(catalog, textFont, screens, themes, logger, false).write(builder)

        val problems = mutableListOf<String>()
        builder.pack.fonts().forEach { font ->
            font.providers().filterIsInstance<BitMapFontProvider>().forEach { provider ->
                if (provider.ascent() > provider.height()) problems += "${font.key()}: ascent ${provider.ascent()} > altura ${provider.height()}"
            }
        }
        builder.pack.textures().filter { it.key().value().startsWith("ui_gen/") }.forEach { texture ->
            val image = ImageIO.read(ByteArrayInputStream(texture.data().toByteArray()))
            if (image.width > 256 || image.height > 256) problems += "${texture.key()}: ${image.width}x${image.height}"
        }
        problems.take(10).shouldBeEmpty()
    }
})
