package textura_studio.render

import net.kyori.adventure.key.Key
import one_block.textura.dialog.kit.BaseUiKit
import one_block.textura.dialog.kit.UiArtCatalog
import one_block.textura.dialog.kit.UiKit
import one_block.textura.dialog.kit.UiTextFontLoader
import one_block.textura.dialog.theme.UiTheme
import one_block.textura.pack.PackAssets
import one_block.textura.pack.VanillaAssets
import one_block.textura.pack.parseLang
import one_block.textura.registry.UiThemeRegistry
import org.bukkit.configuration.file.YamlConfiguration
import textura_studio.StudioLog
import java.io.File

/**
 * Tudo que o preview precisa ler do disco, montado uma vez e reaproveitado entre
 * pedidos. Nada aqui grava nos arquivos do servidor.
 */
class StudioAssets(
    val themes: Map<String, UiTheme>,
    val catalog: UiArtCatalog,
    val kit: UiKit,
    val bodyFont: McFont,
    val vanillaFont: McFont,
    val files: GameFiles,
    val lang: Map<String, String>,
    val warnings: List<String>,
    val glyphIds: List<String> = emptyList(),
    val uiArt: List<String> = emptyList(),
    val glyphs: Map<String, GlyphMeta> = emptyMap(),
    val bossbarFont: McFont = McFont.EMPTY,
) {

    fun translate(key: String, fallback: String): String = lang[key] ?: fallback

    companion object {

        private const val UI_ART_FOLDER = "assets/oneblock/textures/ui"
        private const val VANILLA_FOLDER = "generated/vanilla"
        private const val TEXT_FONT = "configs/fonts/custom.json"
        private const val LOCALE = "pt_br"
        private const val BOSSBAR_FONT = "configs/fonts/bossbar.json"

        fun load(data: File): StudioAssets {
            val log = StudioLog()
            val packAssets = PackAssets(data)
            val ui = packAssets.configFolder("ui")

            val themes = UiThemeRegistry(log.logger).also { it.load(ui) }.themes
            val vanillaFolder = File(data, VANILLA_FOLDER)
            val jar = GameFiles.newestClientJar(vanillaFolder)
            if (jar == null) log.add("Client jar vanilla não encontrado em $VANILLA_FOLDER; ícones item: e moldura ficam vazios (abra o servidor uma vez com o Textura)")
            val vanilla = jar?.let { VanillaAssets(vanillaFolder, GameFiles.version(it), log.logger) }
            val glyphs = glyphMeta(packAssets, log)

            val catalog = UiArtCatalog(File(packAssets.resourceRoot, UI_ART_FOLDER), { glyphs[it]?.file }, log.logger) { name ->
                vanilla?.itemTexture(name, false)
            }
            val textFont = UiTextFontLoader.load(File(data, TEXT_FONT), packAssets.resourceRoot, log.logger)
            val kit = BaseUiKit(catalog, textFont)

            val files = GameFiles(packAssets.resourceRoot, jar)
            val loader = McFontLoader(files::fontJson, files::fontTexture) { log.add(it) }
            val customJson = File(data, TEXT_FONT).takeIf { it.isFile }?.readText()
            val bodyFont = customJson?.let { loader.loadText(it, TEXT_FONT) } ?: McFont.EMPTY
            val vanillaFont = loader.load(Key.key("minecraft", "default"))
            val lang = jar?.let { File(vanillaFolder, "lang-${GameFiles.version(it)}-$LOCALE.json") }
                ?.takeIf { it.isFile }
                ?.let { parseLang(it.readText()) }
                .orEmpty() + File(packAssets.resourceRoot, "assets/minecraft/lang/$LOCALE.json")
                .takeIf { it.isFile }
                ?.let { parseLang(it.readText()) }
                .orEmpty()
            val bossbarFont = File(data, BOSSBAR_FONT).takeIf { it.isFile }?.let { loader.loadText(it.readText(), BOSSBAR_FONT) } ?: vanillaFont

            val uiRoot = File(packAssets.resourceRoot, UI_ART_FOLDER)
            val uiArt = uiRoot.walkTopDown()
                .filter { it.isFile && it.extension == "png" }
                .map { "ui:" + it.relativeTo(uiRoot).invariantSeparatorsPath.removeSuffix(".png") }
                .sorted()
                .toList()
            return StudioAssets(themes, catalog, kit, bodyFont, vanillaFont, files, lang, log.all(), glyphs.keys.sorted(), uiArt, glyphs, bossbarFont)
        }

        /** Mesmo mapeamento id -> arquivo do GlyphRegistry, só que somente leitura. */
        private fun glyphMeta(assets: PackAssets, log: StudioLog): Map<String, GlyphMeta> {
            val folder = assets.configFolder("glyphs")
            if (!folder.isDirectory) return emptyMap()
            return folder.walkTopDown()
                .filter { it.isFile && it.extension in setOf("yml", "yaml") }
                .flatMap { file ->
                    val config = runCatching { YamlConfiguration.loadConfiguration(file) }.getOrElse {
                        log.add("Glyphs '${file.name}' ilegível: ${it.message}")
                        return@flatMap emptySequence()
                    }
                    config.getKeys(false).asSequence().mapNotNull { key ->
                        val caminho = config.getString("$key.caminho") ?: return@mapNotNull null
                        if (config.isList("$key.ids")) return@mapNotNull null
                        val id = config.getString("$key.id") ?: key
                        assets.resolveGlyph(caminho, folder)?.file?.let { file ->
                            id to GlyphMeta(
                                id = id,
                                file = file,
                                ascent = config.getInt("$key.ascent", GlyphMeta.DEFAULT_ASCENT),
                                height = config.getInt("$key.height", GlyphMeta.DEFAULT_HEIGHT),
                                size = config.getInt("$key.size", 0).takeIf { it > 0 },
                            )
                        }
                    }
                }
                .toMap()
        }
    }
}

/** Dados de um glyph simples do `configs/glyphs` que o preview precisa para desenhá-lo. */
data class GlyphMeta(val id: String, val file: File, val ascent: Int, val height: Int, val size: Int?) {
    companion object {
        const val DEFAULT_ASCENT = 8
        const val DEFAULT_HEIGHT = 8
    }
}
