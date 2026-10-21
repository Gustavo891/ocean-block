package textura_studio.render

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.kyori.adventure.key.Key
import java.awt.image.BufferedImage

/** Um caractere de fonte bitmap do Minecraft, já recortado da folha. */
class FontGlyph(val image: BufferedImage, val height: Int, val ascent: Int, val advance: Int) {
    val drawWidth: Double get() = image.width * height.toDouble() / image.height
}

class McFont(private val glyphs: Map<Int, FontGlyph>, private val spaces: Map<Int, Int>) {

    fun glyph(codePoint: Int): FontGlyph? = glyphs[codePoint]

    fun advance(codePoint: Int): Int = spaces[codePoint] ?: glyphs[codePoint]?.advance ?: MISSING_ADVANCE

    fun width(text: String): Int = text.codePoints().toArray().sumOf(::advance)

    companion object {
        const val MISSING_ADVANCE = 6
        val EMPTY = McFont(emptyMap(), emptyMap())
    }
}

/**
 * Lê fontes no formato do resource pack (`bitmap`, `space`, `reference`), com a mesma
 * regra de largura do cliente: pixels visíveis × escala, arredondado, + 1.
 */
class McFontLoader(
    private val fontJson: (Key) -> String?,
    private val texture: (Key) -> BufferedImage?,
    private val warn: (String) -> Unit,
) {

    private class Sink {
        val glyphs = LinkedHashMap<Int, FontGlyph>()
        val spaces = LinkedHashMap<Int, Int>()
    }

    fun load(id: Key): McFont {
        val sink = Sink()
        visit(id, sink, 0)
        return McFont(sink.glyphs, sink.spaces)
    }

    fun loadText(json: String, label: String): McFont {
        val sink = Sink()
        providers(json, label, sink, 0)
        return McFont(sink.glyphs, sink.spaces)
    }

    private fun visit(id: Key, sink: Sink, depth: Int) {
        if (depth > MAX_DEPTH) return
        val json = fontJson(id) ?: run {
            warn("Fonte '$id' não encontrada")
            return
        }
        providers(json, id.asString(), sink, depth)
    }

    private fun providers(json: String, label: String, sink: Sink, depth: Int) {
        val root = runCatching { JsonParser.parseString(json).asJsonObject }.getOrElse {
            warn("Fonte '$label' inválida: ${it.message}")
            return
        }
        root.getAsJsonArray("providers")?.forEach { element ->
            val provider = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
            when (provider.get("type")?.asString) {
                "bitmap" -> bitmap(provider, sink)
                "space" -> provider.getAsJsonObject("advances")?.entrySet()?.forEach { (char, advance) ->
                    char.codePoints().findFirst().ifPresent { sink.spaces.putIfAbsent(it, advance.asInt) }
                }
                "reference" -> provider.get("id")?.asString?.let { visit(Key.key(it), sink, depth + 1) }
            }
        }
    }

    private fun bitmap(provider: JsonObject, sink: Sink) {
        val file = provider.get("file")?.asString ?: return
        val rows = provider.getAsJsonArray("chars")?.map { it.asString.codePoints().toArray() } ?: return
        val height = provider.get("height")?.asInt ?: DEFAULT_HEIGHT
        val ascent = provider.get("ascent")?.asInt ?: DEFAULT_ASCENT
        val sheet = texture(Key.key(file)) ?: run {
            warn("Textura de fonte '$file' não encontrada")
            return
        }
        val columns = rows.maxOfOrNull { it.size } ?: return
        if (columns == 0 || rows.isEmpty()) return

        val cellWidth = sheet.width / columns
        val cellHeight = sheet.height / rows.size
        val scale = height.toFloat() / cellHeight

        rows.forEachIndexed { row, codePoints ->
            codePoints.forEachIndexed { column, codePoint ->
                if (codePoint == 0 || sink.glyphs.containsKey(codePoint)) return@forEachIndexed
                val cell = sheet.getSubimage(column * cellWidth, row * cellHeight, cellWidth, cellHeight)
                val advance = (0.5f + visibleWidth(cell) * scale).toInt() + 1
                sink.glyphs[codePoint] = FontGlyph(cell, height, ascent, advance)
            }
        }
    }

    private fun visibleWidth(cell: BufferedImage): Int {
        for (x in cell.width - 1 downTo 0) {
            for (y in 0 until cell.height) {
                if ((cell.getRGB(x, y) ushr 24) != 0) return x + 1
            }
        }
        return 0
    }

    private companion object {
        const val DEFAULT_HEIGHT = 8
        const val DEFAULT_ASCENT = 7
        const val MAX_DEPTH = 8
    }
}
