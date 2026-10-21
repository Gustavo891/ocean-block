package textura_studio.render

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

data class GlyphRequest(val file: File, val ascent: Int, val height: Int, val size: Int?, val text: String)

/**
 * Uma linha de chat com o glyph no meio, como a fonte padrão o desenha: topo em
 * `linha + 7 - ascent`, altura `height`, avanço `size + 1` (ou a largura medida).
 */
class GlyphPreview(private val assets: StudioAssets) {

    fun render(request: GlyphRequest): BufferedImage {
        val glyph = runCatching { ImageIO.read(request.file) }.getOrNull()
        val (before, after) = request.text.split(MARK, limit = 2).let { it[0] to it.getOrElse(1) { "" } }
        val glyphWidth = glyph?.let { it.width * request.height.toDouble() / it.height } ?: 0.0
        val advance = (request.size ?: kotlin.math.ceil(glyphWidth).toInt()) + 1
        val lineWidth = assets.vanillaFont.width(before) + advance + assets.vanillaFont.width(after)
        val width = lineWidth + PADDING * 2
        val height = maxOf(LINE + PADDING * 2, request.height + PADDING * 2 + maxOf(0, request.ascent - TextPainter.BASE_ASCENT))
        val top = PADDING + maxOf(0, request.ascent - TextPainter.BASE_ASCENT)

        val image = BufferedImage(width * SCALE, height * SCALE, BufferedImage.TYPE_INT_ARGB)
        Canvas(image, SCALE).use { canvas ->
            canvas.fill(0, 0, width, height, CHAT_BACKGROUND)
            val painter = TextPainter(canvas, assets.vanillaFont, null)
            painter.draw(before, PADDING, top, WHITE, false, true)
            val x = PADDING + assets.vanillaFont.width(before)
            glyph?.let { canvas.draw(it, x.toDouble(), (top + TextPainter.BASE_ASCENT - request.ascent).toDouble(), glyphWidth, request.height.toDouble()) }
            painter.draw(after, x + advance, top, WHITE, false, true)
        }
        return image
    }

    private companion object {
        const val MARK = "{glyph}"
        const val SCALE = 4
        const val PADDING = 3
        const val LINE = 9
        const val WHITE = 0xFFFFFF
        const val CHAT_BACKGROUND = 0x80000000.toInt()
    }
}
