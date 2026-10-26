package one_block.textura.dialog.kit

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.kyori.adventure.key.Key
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileReader
import java.util.logging.Logger
import javax.imageio.ImageIO

data class TextProvider(val file: Key, val height: Int, val ascent: Int, val chars: List<String>)

class UiTextFont(val providers: List<TextProvider>, private val widths: Map<Int, Int>, val extras: Map<String, UiTextFont> = emptyMap()) {

    fun advance(codePoint: Int): Int = widths[codePoint] ?: DEFAULT_ADVANCE

    fun of(font: String?): UiTextFont = font?.let { extras[it] } ?: this

    fun withExtras(extras: Map<String, UiTextFont>): UiTextFont = UiTextFont(providers, widths, extras)

    companion object {
        const val DEFAULT_ADVANCE = 6
        const val SPACE_ADVANCE = 4
    }
}

object UiTextFontLoader {

    private const val DEFAULT_HEIGHT = 8
    private const val DEFAULT_ASCENT = 7

    fun load(json: File, resourceRoot: File, logger: Logger): UiTextFont {
        val widths = mutableMapOf(' '.code to UiTextFont.SPACE_ADVANCE)
        if (!json.isFile) {
            logger.warning("[ui] Fonte de texto '${json.path}' não encontrada; larguras padrão de ${UiTextFont.DEFAULT_ADVANCE} px")
            return UiTextFont(emptyList(), widths)
        }

        val root = runCatching { FileReader(json, Charsets.UTF_8).use { JsonParser.parseReader(it).asJsonObject } }.getOrElse {
            logger.warning("[ui] Fonte de texto '${json.path}' inválida (${it.message}); larguras padrão de ${UiTextFont.DEFAULT_ADVANCE} px")
            return UiTextFont(emptyList(), widths)
        }
        val providers = mutableListOf<TextProvider>()

        root.getAsJsonArray("providers")?.forEach { element ->
            val provider = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
            when (provider.get("type")?.takeIf { it.isJsonPrimitive }?.asString) {
                "space" -> provider.getAsJsonObject("advances")?.entrySet()?.forEach { (char, advance) ->
                    char.codePoints().findFirst().ifPresent { widths.putIfAbsent(it, advance.asInt) }
                }
                "bitmap" -> bitmap(provider, resourceRoot, logger)?.let { (textProvider, measured) ->
                    providers += textProvider
                    measured.forEach { (codePoint, advance) -> widths.putIfAbsent(codePoint, advance) }
                }
            }
        }
        return UiTextFont(providers, widths)
    }

    private fun bitmap(provider: JsonObject, resourceRoot: File, logger: Logger): Pair<TextProvider, Map<Int, Int>>? {
        val file = provider.get("file")?.asString ?: return null
        val chars = provider.getAsJsonArray("chars")?.map { it.asString } ?: return null
        val key = Key.key(file)
        val height = provider.get("height")?.asInt ?: DEFAULT_HEIGHT
        val textProvider = TextProvider(key, height, provider.get("ascent")?.asInt ?: DEFAULT_ASCENT, chars)

        val texture = File(resourceRoot, "assets/${key.namespace()}/textures/${key.value()}")
        val image = if (texture.isFile) runCatching { ImageIO.read(texture) }.getOrNull() else null
        if (image == null) {
            logger.warning("[ui] Textura '$key' não está em resource/; larguras desse provider usam ${UiTextFont.DEFAULT_ADVANCE} px")
            return textProvider to emptyMap()
        }

        val rows = chars.map { it.codePoints().toArray() }
        val columns = rows.maxOfOrNull { it.size } ?: 0
        if (columns == 0) return textProvider to emptyMap()

        val cellWidth = image.width / columns
        val cellHeight = image.height / rows.size
        val scale = height.toFloat() / cellHeight
        val widths = mutableMapOf<Int, Int>()

        rows.forEachIndexed { row, codePoints ->
            codePoints.forEachIndexed { column, codePoint ->
                if (codePoint != 0 && codePoint != ' '.code) {
                    val actual = actualWidth(image, column * cellWidth, row * cellHeight, cellWidth, cellHeight)
                    widths.putIfAbsent(codePoint, (0.5f + actual * scale).toInt() + 1)
                }
            }
        }
        return textProvider to widths
    }

    private fun actualWidth(image: BufferedImage, x0: Int, y0: Int, width: Int, height: Int): Int {
        for (x in width - 1 downTo 0) {
            for (y in 0 until height) {
                if ((image.getRGB(x0 + x, y0 + y) ushr 24) != 0) return x + 1
            }
        }
        return 0
    }
}
