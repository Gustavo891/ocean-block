package one_block.textura.dialog.kit

import java.awt.image.BufferedImage

class UiPieceSource(val name: String, val image: BufferedImage, val detail: Int = 1)

object UiPieces {

    private const val MIN_HEIGHT = 8

    val MIDDLE_SIZES: List<Int> = (0..8).map { 1 shl it }

    fun padded(image: BufferedImage): BufferedImage {
        if (image.height >= MIN_HEIGHT) return image

        val result = BufferedImage(image.width, MIN_HEIGHT, BufferedImage.TYPE_INT_ARGB)
        val graphics = result.createGraphics()
        graphics.drawImage(image, 0, 0, null)
        graphics.dispose()
        return result
    }

    fun slices(id: String, image: BufferedImage, left: Int, right: Int): List<UiPieceSource> {
        val middle = image.getSubimage(left, 0, image.width - left - right, image.height)
        val caps = listOfNotNull(
            if (left > 0) UiPieceSource("$id#left", image.getSubimage(0, 0, left, image.height)) else null,
            if (right > 0) UiPieceSource("$id#right", image.getSubimage(image.width - right, 0, right, image.height)) else null,
        )
        return caps + MIDDLE_SIZES.map { size -> UiPieceSource("$id#mid$size", tiled(middle, size)) }
    }

    private fun tiled(middle: BufferedImage, size: Int): BufferedImage {
        val result = BufferedImage(size, middle.height, BufferedImage.TYPE_INT_ARGB)
        val graphics = result.createGraphics()
        var x = 0
        while (x < size) {
            graphics.drawImage(middle, x, 0, null)
            x += middle.width
        }
        graphics.dispose()
        return result
    }
}

object SurfaceComposer {

    fun compose(image: BufferedImage, slice: ArtSlice, width: Int, height: Int): BufferedImage? {
        if (width < 1 || height < 1) return null
        if (width < slice.left + slice.right || height < slice.top + slice.bottom) return null

        val result = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val sx = intArrayOf(0, slice.left, image.width - slice.right, image.width)
        val sy = intArrayOf(0, slice.top, image.height - slice.bottom, image.height)
        val dx = intArrayOf(0, slice.left, width - slice.right, width)
        val dy = intArrayOf(0, slice.top, height - slice.bottom, height)

        for (row in 0..2) {
            val sourceHeight = sy[row + 1] - sy[row]
            if (sourceHeight == 0) continue
            for (column in 0..2) {
                val sourceWidth = sx[column + 1] - sx[column]
                if (sourceWidth == 0) continue
                for (y in dy[row] until dy[row + 1]) {
                    for (x in dx[column] until dx[column + 1]) {
                        val pixel = image.getRGB(sx[column] + (x - dx[column]) % sourceWidth, sy[row] + (y - dy[row]) % sourceHeight)
                        result.setRGB(x, y, pixel)
                    }
                }
            }
        }
        return result
    }
}
