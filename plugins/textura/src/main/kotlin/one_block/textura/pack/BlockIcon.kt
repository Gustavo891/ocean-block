package one_block.textura.pack

import java.awt.image.BufferedImage

data class BlockFaces<T>(val top: T, val front: T, val side: T, val under: T? = null)

object BlockIcon {

    private const val SIZE = 16
    private const val HALF = SIZE / 2
    private const val QUARTER = SIZE / 4
    private const val FRONT_SHADE = 0.8
    private const val SIDE_SHADE = 0.6

    fun render(faces: BlockFaces<BufferedImage>): BufferedImage {
        val result = BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB)
        for (px in 0 until SIZE) {
            for (py in 0 until SIZE) {
                pixel(faces, px + 0.5, py + 0.5)?.let { result.setRGB(px, py, it) }
            }
        }
        return result
    }

    private fun pixel(faces: BlockFaces<BufferedImage>, x: Double, y: Double): Int? {
        val u = (x / HALF - (y - QUARTER) / QUARTER) / 2
        val v = (x / HALF + (y - QUARTER) / QUARTER) / 2
        if (u in 0.0..<1.0 && v in 0.0..<1.0) return top(faces, u, v)
        return if (x < HALF) {
            val s = x / HALF
            val t = (y - QUARTER - QUARTER * s) / HALF
            if (t in 0.0..<1.0) shade(sample(faces.front, s, t), FRONT_SHADE) else null
        } else {
            val s = (x - HALF) / HALF
            val t = (y - HALF + QUARTER * s) / HALF
            if (t in 0.0..<1.0) shade(sample(faces.side, s, t), SIDE_SHADE) else null
        }
    }

    private fun top(faces: BlockFaces<BufferedImage>, u: Double, v: Double): Int {
        val color = sample(faces.top, u, v)
        if (color ushr 24 != 0) return color
        return faces.under?.let { sample(it, u, v) } ?: color
    }

    private fun sample(image: BufferedImage, s: Double, t: Double): Int {
        val x = (s * image.width).toInt().coerceIn(0, image.width - 1)
        val y = (t * image.height).toInt().coerceIn(0, image.height - 1)
        return image.getRGB(x, y)
    }

    private fun shade(argb: Int, factor: Double): Int {
        val r = ((argb shr 16 and 0xFF) * factor).toInt()
        val g = ((argb shr 8 and 0xFF) * factor).toInt()
        val b = ((argb and 0xFF) * factor).toInt()
        return (argb and 0xFF000000.toInt()) or (r shl 16) or (g shl 8) or b
    }
}
