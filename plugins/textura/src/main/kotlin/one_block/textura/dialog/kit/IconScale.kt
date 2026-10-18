package one_block.textura.dialog.kit

import java.awt.RenderingHints
import java.awt.image.BufferedImage

object IconScale {

    fun width(width: Int, height: Int, size: Int): Int =
        maxOf(1, Math.round(width * size / height.coerceAtLeast(1).toDouble()).toInt())

    fun native(image: BufferedImage, size: Int): BufferedImage? {
        var minX = image.width
        var minY = image.height
        var maxX = -1
        var maxY = -1
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                if (image.getRGB(x, y) ushr 24 == 0) continue
                minX = minOf(minX, x)
                minY = minOf(minY, y)
                maxX = maxOf(maxX, x)
                maxY = maxOf(maxY, y)
            }
        }
        if (maxX < 0) return null
        val width = maxX - minX + 1
        val height = maxY - minY + 1
        if (width > size || height > size) return null

        val result = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val graphics = result.createGraphics()
        graphics.drawImage(image.getSubimage(minX, minY, width, height), (size - width) / 2, size - height, null)
        graphics.dispose()
        return result
    }

    fun resize(image: BufferedImage, size: Int): BufferedImage? {
        if (size < 1) return null
        val width = width(image.width, image.height, size)
        val result = BufferedImage(width, size, BufferedImage.TYPE_INT_ARGB)
        val graphics = result.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
        graphics.drawImage(image, 0, 0, width, size, null)
        graphics.dispose()
        return result
    }
}
