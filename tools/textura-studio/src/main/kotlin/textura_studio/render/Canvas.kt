package textura_studio.render

import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Shape
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.util.IdentityHashMap

/**
 * Desenha em coordenadas de GUI do Minecraft numa imagem do tamanho do monitor:
 * cada pixel de GUI vira `scale` pixels reais, como no jogo.
 */
class Canvas(val image: BufferedImage, val scale: Int) : AutoCloseable {

    private val graphics: Graphics2D = image.createGraphics().apply {
        setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
        setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED)
        composite = AlphaComposite.SrcOver
    }
    private var savedClip: Shape? = null

    fun draw(source: BufferedImage, x: Double, y: Double, width: Double = source.width.toDouble(), height: Double = source.height.toDouble()) {
        if (width <= 0 || height <= 0) return
        val transform = AffineTransform()
        transform.translate(x * scale, y * scale)
        transform.scale(width * scale / source.width, height * scale / source.height)
        graphics.drawImage(source, transform, null)
    }

    fun draw(source: BufferedImage, x: Int, y: Int) = draw(source, x.toDouble(), y.toDouble())

    fun fill(x: Int, y: Int, width: Int, height: Int, argb: Int) {
        graphics.color = Color(argb, true)
        graphics.fillRect(x * scale, y * scale, width * scale, height * scale)
    }

    fun gradient(top: Int, bottom: Int, width: Int, height: Int) {
        graphics.paint = java.awt.GradientPaint(0f, 0f, Color(top), 0f, (height * scale).toFloat(), Color(bottom))
        graphics.fillRect(0, 0, width * scale, height * scale)
    }

    fun clip(x: Int, y: Int, width: Int, height: Int) {
        savedClip = graphics.clip
        graphics.clipRect(x * scale, y * scale, width * scale, height * scale)
    }

    fun unclip() {
        graphics.clip = savedClip
        savedClip = null
    }

    override fun close() = graphics.dispose()
}

/** Pinta texto com a mesma regra do cliente: sombra 1 px a 25% da cor e negrito com cópia +1 px. */
class TextPainter(private val canvas: Canvas, private val font: McFont, private val fallback: McFont?) {

    private val tinted = IdentityHashMap<BufferedImage, HashMap<Int, BufferedImage>>()

    fun draw(text: String, x: Int, y: Int, rgb: Int, bold: Boolean, shadow: Boolean, advance: (Int) -> Int = font::advance) {
        if (shadow) pass(text, x + 1, y + 1, shadowOf(rgb), bold, advance)
        pass(text, x, y, rgb, bold, advance)
    }

    fun width(text: String, bold: Boolean): Int = text.codePoints().toArray().sumOf { font.advance(it) + if (bold) 1 else 0 }

    private fun pass(text: String, x: Int, y: Int, rgb: Int, bold: Boolean, advance: (Int) -> Int) {
        var cursor = x
        text.codePoints().forEach { codePoint ->
            val glyph = if (codePoint == ' '.code) null else font.glyph(codePoint) ?: fallback?.glyph(codePoint)
            if (glyph != null) {
                val image = tint(glyph.image, rgb)
                val top = y + BASE_ASCENT - glyph.ascent.toDouble()
                canvas.draw(image, cursor.toDouble(), top, glyph.drawWidth, glyph.height.toDouble())
                if (bold) canvas.draw(image, cursor + 1.0, top, glyph.drawWidth, glyph.height.toDouble())
            }
            cursor += advance(codePoint) + if (bold) 1 else 0
        }
    }

    private fun tint(source: BufferedImage, rgb: Int): BufferedImage {
        if (rgb and 0xFFFFFF == 0xFFFFFF) return source
        return tinted.getOrPut(source) { HashMap() }.getOrPut(rgb) {
            val result = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_ARGB)
            val r = rgb shr 16 and 0xFF
            val g = rgb shr 8 and 0xFF
            val b = rgb and 0xFF
            for (py in 0 until source.height) {
                for (px in 0 until source.width) {
                    val argb = source.getRGB(px, py)
                    val alpha = argb ushr 24
                    if (alpha == 0) continue
                    val nr = (argb shr 16 and 0xFF) * r / 255
                    val ng = (argb shr 8 and 0xFF) * g / 255
                    val nb = (argb and 0xFF) * b / 255
                    result.setRGB(px, py, (alpha shl 24) or (nr shl 16) or (ng shl 8) or nb)
                }
            }
            result
        }
    }

    companion object {
        const val BASE_ASCENT = 7
        const val WHITE = 0xFFFFFF

        fun shadowOf(rgb: Int): Int = (rgb and 0xFCFCFC) shr 2
    }
}
