package ocean_block.system.islands.dialog

import java.awt.image.BufferedImage

data class MapColumn(val rgb: Int, val height: Int, val waterDepth: Int)

object IslandMapImage {

    const val LIGHT = 255
    const val BASE = 220
    const val DARK = 180
    const val MARKER = 0xFFFFFFFF.toInt()
    const val MARKER_EDGE = 0xFF1A1A1A.toInt()

    private const val SHALLOW = 2
    private const val MEDIUM = 6

    fun render(grid: List<List<MapColumn>>, marker: Pair<Int, Int>?): BufferedImage {
        val height = grid.size
        val width = grid.firstOrNull()?.size ?: 0
        val image = BufferedImage(width.coerceAtLeast(1), height.coerceAtLeast(1), BufferedImage.TYPE_INT_ARGB)
        grid.forEachIndexed { z, row ->
            row.forEachIndexed { x, column ->
                image.setRGB(x, z, shade(column.rgb, level(column, grid.getOrNull(z - 1)?.getOrNull(x))))
            }
        }
        marker?.let { (mx, mz) -> drawMarker(image, mx, mz) }
        return image
    }

    fun shade(rgb: Int, level: Int): Int {
        val r = ((rgb shr 16) and 0xFF) * level / 255
        val g = ((rgb shr 8) and 0xFF) * level / 255
        val b = (rgb and 0xFF) * level / 255
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun level(column: MapColumn, north: MapColumn?): Int = when {
        column.waterDepth > 0 -> when {
            column.waterDepth <= SHALLOW -> LIGHT
            column.waterDepth <= MEDIUM -> BASE
            else -> DARK
        }
        north == null || north.height == column.height -> BASE
        column.height > north.height -> LIGHT
        else -> DARK
    }

    private fun drawMarker(image: BufferedImage, mx: Int, mz: Int) {
        for (dx in -2..2) {
            for (dz in -2..2) {
                val x = mx + dx
                val z = mz + dz
                if (x !in 0 until image.width || z !in 0 until image.height) continue
                val inner = dx in -1..1 && dz in -1..1
                image.setRGB(x, z, if (inner) MARKER else MARKER_EDGE)
            }
        }
    }
}
