package one_block.textura.dialog

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import one_block.textura.api.DialogBinds
import one_block.textura.dialog.json.ScreenCompiler
import one_block.textura.dialog.json.ScreenParser
import one_block.textura.dialog.kit.BaseUiKit
import one_block.textura.dialog.kit.UiArtCatalog
import one_block.textura.dialog.kit.UiTextFontLoader
import one_block.textura.dialog.layout.UiLayout
import one_block.textura.registry.UiThemeRegistry
import java.io.File
import java.util.logging.Logger

class ExistingScreensSnapshotTest : StringSpec({

    val screens = listOf("settings", "shop", "social")
    val snapshot = "/dialog/existing-screens.snapshot"

    fun dump(): String {
        val data = File(File(System.getProperty("user.dir")).parentFile.parentFile, "server/plugins/Textura")
        val logger = Logger.getAnonymousLogger().apply { useParentHandlers = false }
        val ui = File(data, "configs/ui")
        val themes = UiThemeRegistry(logger).also { it.load(ui) }.themes
        val catalog = UiArtCatalog(File(data, "resource/assets/oneblock/textures/ui"), { null }, logger)
        val kit = BaseUiKit(catalog, UiTextFontLoader.load(File(data, "configs/fonts/custom.json"), File(data, "resource"), logger))
        val out = StringBuilder()
        screens.forEach { id ->
            val definition = checkNotNull(ScreenParser(logger).parse(File(ui, "$id.json"))) { "tela $id ilegível" }
            val pages: List<String?> = definition.pages.keys.toList().ifEmpty { listOf(null) }
            pages.forEach { page ->
                val session = DialogSession(definition.id, DialogBinds()).also {
                    it.page = page
                    it.defaults = definition.defaults
                    it.choices = definition.choices
                }
                val screen = ScreenCompiler(kit) { }.compile(definition, themes.getValue(definition.theme), session)
                val laid = UiLayout(kit).layout(screen)
                out.append("== $id page=$page\nsize ${laid.width}x${laid.height}\n")
                laid.images.forEach { out.append("img $it\n") }
                laid.texts.forEach { out.append("txt $it\n") }
                laid.hotspots.forEach { out.append("hot ${it.x},${it.y},${it.w},${it.h} action=${it.action != null} tip=${it.tooltip?.asHoverEvent()}\n") }
            }
        }
        return out.toString()
    }

    "shop, settings e social desenham igual ao snapshot de antes das listas" {
        val current = dump()
        System.getenv("TEXTURA_SNAPSHOT_WRITE")?.let { File(it).writeText(current) }
        val expected = checkNotNull(javaClass.getResource(snapshot)) { "snapshot $snapshot ausente" }.readText().replace("\r\n", "\n")

        current shouldBe expected
    }
})
