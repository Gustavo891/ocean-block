package one_block.textura.pack

import com.google.gson.JsonParser
import net.kyori.adventure.key.Key
import one_block.textura.model.FontData
import one_block.textura.registry.FontRegistry
import one_block.textura.registry.GlyphRegistry
import one_block.textura.registry.TitleFontRegistry
import team.unnamed.creative.font.Font
import team.unnamed.creative.font.FontProvider
import team.unnamed.creative.base.Writable
import java.io.File
import java.io.FileReader
import java.util.logging.Level
import java.util.logging.Logger
import one_block.textura.text.SpacingFont

/**
 * Grava tudo que é texto no pack: as fontes nomeadas dos JSONs de `configs/fonts`,
 * a fonte de espaçamento e os glyphs (simples e folhas).
 *
 * A ordem importa. As fontes nomeadas precisam ser escritas ANTES dos glyphs:
 * [PackBuilder.addFont] substitui a fonte inteira, enquanto [PackBuilder.addGlyph]
 * faz merge com os providers existentes.
 */
class FontPackWriter(
    private val glyphRegistry: GlyphRegistry,
    private val fontRegistry: FontRegistry,
    private val titleFontRegistry: TitleFontRegistry,
    private val assets: PackAssets,
    private val dataFolder: File,
    private val logger: Logger,
) {

    fun write(builder: PackBuilder) {
        writeFonts(builder)
        writeSpacingFont(builder)
        writeGlyphs(builder)
    }

    private fun titleFonts(): List<FontData> = titleFontRegistry.getRequested().map { font ->
        FontData(
            key = font.id,
            id = font.id,
            caminho = font.source.path,
            ascent = font.ascent,
            height = font.height,
            baseAscent = font.source.baseAscent,
            absoluteHeight = !font.source.isDefault,
        )
    }

    private fun writeFonts(builder: PackBuilder) {
        val fonts = (fontRegistry.fonts.values + titleFonts()).associateBy { it.id }.values
        for (font in fonts) {
            runCatching { readFontProviders(font) }
                .onSuccess { providers -> if (providers.isNotEmpty()) builder.addFont(font.fontKey, providers) }
                .onFailure { logger.log(Level.WARNING, "Fonte '${font.id}' ignorada no pack", it) }
        }
    }

    /**
     * Resolve o JSON de fonte. `caminho` é relativo à pasta de dados no layout
     * antigo (`fonts/custom.json`) e à pasta de configs no novo. O JSON é
     * definição, não asset: é interpretado, nunca copiado.
     */
    private fun resolveFontJson(caminho: String): File? {
        val legado = File(dataFolder, caminho)
        if (legado.isFile) return legado

        val novo = File(File(dataFolder, "configs"), caminho)
        if (novo.isFile) return novo

        val semPrefixo = File(assets.configFolder("fonts"), caminho.substringAfterLast('/'))
        return semPrefixo.takeIf { it.isFile }
    }

    private fun readFontProviders(font: FontData): List<FontProvider> {
        val sourceFontJson = resolveFontJson(font.caminho) ?: return emptyList()

        val providers = mutableListOf<FontProvider>()
        val spaces = mutableListOf<FontProvider>()
        FileReader(sourceFontJson, Charsets.UTF_8).use { reader ->
            val fontJson = JsonParser.parseReader(reader).asJsonObject
            if (!fontJson.has("providers")) return emptyList()

            for (element in fontJson.getAsJsonArray("providers")) {
                val prov = element.asJsonObject
                when (val type = prov.get("type")?.asString) {
                    "bitmap" -> Unit
                    "space" -> {
                        prov.getAsJsonObject("advances")?.entrySet()
                            ?.associate { (char, advance) -> char to advance.asInt }
                            ?.takeIf { it.isNotEmpty() }
                            ?.let { spaces.add(FontProvider.space(it)) }
                        continue
                    }
                    else -> {
                        logger.warning("Fonte '${font.id}': provider '$type' nao suportado e ignorado.")
                        continue
                    }
                }

                val fileStr = prov.get("file")?.takeIf { !it.isJsonNull }?.asString ?: continue
                val charsArray = prov.getAsJsonArray("chars") ?: continue

                val fileKey = parseFileKey(fileStr)
                val originalHeight = prov.get("height")?.asInt ?: GLYPH_HEIGHT
                // Escala proporcional: font.height é a altura desejada para os glyphs
                // de 8px, os demais providers encolhem na mesma proporção.
                val height = when {
                    font.height == null -> originalHeight
                    font.absoluteHeight -> font.height
                    else -> Math.round(originalHeight * font.height.toFloat() / GLYPH_HEIGHT)
                }
                // Aplica o ascent da config como deslocamento relativo à base vanilla (7),
                // preservando a diferença original entre os providers. Ex.: ascent=3 => shift -4,
                // ascii 7->3 e accented 10->6.
                val originalAscent = prov.get("ascent")?.asInt ?: BASE_ASCENT
                val ascent = originalAscent + (font.ascent - font.baseAscent)
                val chars = charsArray.map { it.asString }

                if (ascent > height) {
                    logger.warning("Fonte '${font.id}': provider '$fileStr' com ascent $ascent maior que height $height; ignorado.")
                    continue
                }
                providers.add(FontProvider.bitMap(fileKey, height, ascent, chars))
            }
        }
        if (providers.isEmpty()) return providers
        return listOf(FontProvider.space(mapOf(" " to SPACE_ADVANCE))) + spaces + providers
    }

    /**
     * Fonte de espaçamento. Os chars vão como escape de propósito: eles vivem na
     * Private Use Area e, escritos como literais, ficam invisíveis no editor — foi
     * assim que dois deles trocaram de lugar numa refatoração e quebraram a emenda
     * da HUD. Este mapa é o contrato de [one_block.textura.text.TextMetrics.getSpacingComponent]
     * e precisa casar com ele exatamente.
     */
    private fun writeSpacingFont(builder: PackBuilder) {
        builder.addFont(SpacingFont.KEY, listOf(FontProvider.space(SpacingFont.ADVANCES)))
    }

    private fun writeGlyphs(builder: PackBuilder) {
        val glyphsFolder = assets.configFolder("glyphs")
        val (fatias, simples) = glyphRegistry.glyphs.values.partition { it.sheetKey != null }

        for (glyph in simples) isolated("Glyph '${glyph.id}'") {
            val asset = assets.resolveGlyph(glyph.caminho, glyphsFolder) ?: continue

            // A chave da textura precisa manter a extensao (.png): o creative grava o
            // arquivo no caminho da chave e o MC resolve "file" da fonte como textures/<file>.
            val textureKey = asset.keyWithExtension
            val writable = Writable.file(asset.file)
            // Textura ja copiada por resource/ nao pode ser reescrita (colisao no zip).
            val writeTexture = !asset.inResource

            // Fonte global (chat/textos), só quando o glyph pede default.
            if (glyph.default) {
                builder.addGlyph(
                    textureKey = textureKey,
                    texture = writable,
                    char = glyph.char,
                    ascent = glyph.ascent,
                    height = glyph.height,
                    fontKey = Font.MINECRAFT_DEFAULT,
                    writeTexture = writeTexture
                )
            }

            // Fontes nomeadas (ex.: minecraft:bossbar), com ascent/height próprios.
            for (font in glyph.fonts.values) {
                builder.addGlyph(
                    textureKey = textureKey,
                    texture = writable,
                    char = font.char,
                    ascent = font.ascent,
                    height = font.height,
                    fontKey = font.fontKey,
                    writeTexture = writeTexture
                )
            }
        }

        // Uma folha vira um provider só por fonte, com os chars na ordem das linhas —
        // é a lista de chars que faz o cliente fatiar a textura.
        for ((sheet, grupo) in fatias.groupBy { it.sheetKey }) isolated("Folha '$sheet'") {
            val ordenadas = grupo.sortedBy { it.sheetIndex }
            val primeira = ordenadas.first()
            val asset = assets.resolveGlyph(primeira.caminho, glyphsFolder) ?: continue
            val textureKey = asset.keyWithExtension
            val writable = Writable.file(asset.file)
            val writeTexture = !asset.inResource

            if (primeira.default) {
                builder.addGlyphSheet(
                    textureKey = textureKey,
                    texture = writable,
                    chars = ordenadas.map { it.char },
                    ascent = primeira.ascent,
                    height = primeira.height,
                    fontKey = Font.MINECRAFT_DEFAULT,
                    writeTexture = writeTexture
                )
            }

            for (nome in primeira.fonts.keys) {
                val fontes = ordenadas.mapNotNull { it.fonts[nome] }
                if (fontes.size != ordenadas.size) {
                    logger.warning(
                        "Folha '${primeira.sheetKey}' nao registra a fonte '$nome' em todas as fatias; fonte ignorada."
                    )
                    continue
                }
                val referencia = fontes.first()
                builder.addGlyphSheet(
                    textureKey = textureKey,
                    texture = writable,
                    chars = fontes.map { it.char },
                    ascent = referencia.ascent,
                    height = referencia.height,
                    fontKey = referencia.fontKey,
                    writeTexture = writeTexture
                )
            }
        }
    }

    private inline fun isolated(what: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            logger.log(Level.WARNING, "$what ignorado no pack", e)
        }
    }

    private fun parseFileKey(fileStr: String): Key {
        // Mantem a extensao: a chave deve casar com o caminho real da textura no pack.
        return if (fileStr.contains(":")) {
            Key.key(fileStr.substringBefore(":"), fileStr.substringAfter(":"))
        } else {
            Key.key("minecraft", fileStr)
        }
    }

    private companion object {
        const val GLYPH_HEIGHT = 8

        /** Ascent padrão do texto vanilla, base para os deslocamentos relativos de fonte. */
        const val BASE_ASCENT = 7

        /** Avanço do espaço na fonte vanilla, reposto porque o provider bitmap não o cobre. */
        const val SPACE_ADVANCE = 4
    }
}
