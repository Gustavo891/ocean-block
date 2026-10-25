package ocean_block.system.islands.dialog

import java.awt.image.BufferedImage

object DefaultSkin {

    private const val SKIN = 0xFFB07A55.toInt()
    private const val HAIR = 0xFF3B2A1E.toInt()
    private const val SHIRT = 0xFF2E8B8B.toInt()
    private const val PANTS = 0xFF34356B.toInt()
    private const val SHOES = 0xFF4A4A4A.toInt()
    private const val EYE = 0xFFFFFFFF.toInt()
    private const val PUPIL = 0xFF3A3A9A.toInt()

    fun image(): BufferedImage = BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB).apply {
        fun fill(x: Int, y: Int, w: Int, h: Int, argb: Int) {
            for (i in x until x + w) for (j in y until y + h) setRGB(i, j, argb)
        }
        fill(0, 0, 32, 16, SKIN)
        fill(0, 0, 32, 10, HAIR)
        fill(8, 8, 8, 2, HAIR)
        fill(9, 12, 2, 1, EYE)
        fill(13, 12, 2, 1, EYE)
        fill(10, 12, 1, 1, PUPIL)
        fill(13, 12, 1, 1, PUPIL)
        fill(16, 16, 24, 16, SHIRT)
        fill(40, 16, 16, 16, SKIN)
        fill(40, 16, 16, 7, SHIRT)
        fill(32, 48, 16, 16, SKIN)
        fill(32, 48, 16, 7, SHIRT)
        fill(0, 16, 16, 16, PANTS)
        fill(0, 28, 16, 4, SHOES)
        fill(16, 48, 16, 16, PANTS)
        fill(16, 60, 16, 4, SHOES)
    }
}
