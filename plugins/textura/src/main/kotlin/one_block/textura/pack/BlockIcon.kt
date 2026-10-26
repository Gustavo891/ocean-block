package one_block.textura.pack

import java.awt.image.BufferedImage
import kotlin.math.cos
import kotlin.math.sqrt

data class BlockFaces<T>(val top: T, val front: T, val side: T, val under: T? = null)

object BlockIcon {

    private const val SIZE = 16
    private const val FRONT_SHADE = 0.8
    private const val SIDE_SHADE = 0.6
    private const val GUI_SCALE = 0.625
    private val TILT = cos(Math.PI / 6)

    fun render(faces: BlockFaces<BufferedImage>, size: Int = SIZE): BufferedImage {
        val result = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val cube = Cube(size)
        for (px in 0 until size) {
            for (py in 0 until size) {
                pixel(faces, cube, px + 0.5, py + 0.5)?.let { result.setRGB(px, py, it) }
            }
        }
        return result
    }

    private class Cube(size: Int) {
        private val edge = size * GUI_SCALE
        val width = edge * sqrt(2.0)
        val topHeight = width / 2
        val wall = edge * TILT
        val centerX = size / 2.0
        val top = (size - topHeight - wall) / 2
    }

    private fun pixel(faces: BlockFaces<BufferedImage>, cube: Cube, x: Double, y: Double): Int? {
        val half = cube.width / 2
        val quarter = cube.topHeight / 2
        face(x, y, cube.centerX, cube.top, half, quarter, -half, quarter)?.let { (u, v) -> return top(faces, u, v) }
        face(x, y, cube.centerX - half, cube.top + quarter, half, quarter, 0.0, cube.wall)?.let { (s, t) ->
            return shade(sample(faces.front, s, t), FRONT_SHADE)
        }
        face(x, y, cube.centerX, cube.top + cube.topHeight, half, -quarter, 0.0, cube.wall)?.let { (s, t) ->
            return shade(sample(faces.side, s, t), SIDE_SHADE)
        }
        return null
    }

    private fun face(x: Double, y: Double, ox: Double, oy: Double, ax: Double, ay: Double, bx: Double, by: Double): Pair<Double, Double>? {
        val det = ax * by - ay * bx
        val dx = x - ox
        val dy = y - oy
        val s = (dx * by - dy * bx) / det
        val t = (ax * dy - ay * dx) / det
        return if (s in 0.0..<1.0 && t in 0.0..<1.0) s to t else null
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
