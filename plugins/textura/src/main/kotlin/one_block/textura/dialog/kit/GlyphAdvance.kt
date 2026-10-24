package one_block.textura.dialog.kit

import java.awt.image.BufferedImage

object GlyphAdvance {

    fun of(image: BufferedImage, detail: Int = 1): Int {
        for (x in image.width - 1 downTo 0) {
            for (y in 0 until image.height) {
                if ((image.getRGB(x, y) ushr 24) != 0) return Math.floorDiv(2 * (x + 1) + detail, 2 * detail) + 1
            }
        }
        return 1
    }
}
