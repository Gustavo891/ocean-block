package one_block.textura.pack

import java.awt.RenderingHints
import java.awt.image.BufferedImage

object IconResample {

    fun toMax(image: BufferedImage, max: Int): BufferedImage {
        var current = image
        while (current.width > max || current.height > max) {
            val scale = maxOf(0.5, max / maxOf(current.width, current.height).toDouble())
            current = smooth(current, maxOf(1, (current.width * scale).toInt()), maxOf(1, (current.height * scale).toInt()))
        }
        return current
    }

    fun smooth(image: BufferedImage, width: Int, height: Int): BufferedImage {
        val result = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = result.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        graphics.drawImage(image, 0, 0, width, height, null)
        graphics.dispose()
        return result
    }
}
