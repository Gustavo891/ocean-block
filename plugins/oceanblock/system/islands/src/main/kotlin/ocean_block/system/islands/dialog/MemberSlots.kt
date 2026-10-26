package ocean_block.system.islands.dialog

import java.awt.image.BufferedImage

object MemberSlots {

    const val SLOT = 18
    const val PLUS = 0xFF8E9196.toInt()

    private const val FACE = 8
    private const val ZOOM = 2
    private const val INSET = 1
    private const val PLUS_ARM = 3

    fun face(skin: BufferedImage): BufferedImage {
        val face = BufferedImage(FACE, FACE, BufferedImage.TYPE_INT_ARGB)
        for (x in 0 until FACE) {
            for (y in 0 until FACE) {
                val hat = skin.getRGB(40 + x, 8 + y)
                face.setRGB(x, y, if ((hat ushr 24) >= OPAQUE) hat or OPAQUE_MASK else skin.getRGB(8 + x, 8 + y) or OPAQUE_MASK)
            }
        }
        return face
    }

    fun row(faces: List<BufferedImage?>, total: Int): BufferedImage {
        val image = BufferedImage(total * SLOT, SLOT, BufferedImage.TYPE_INT_ARGB)
        (0 until total).forEach { index ->
            val face = faces.getOrNull(index)
            if (face == null) plus(image, index * SLOT) else head(image, index * SLOT, face)
        }
        return image
    }

    private fun head(image: BufferedImage, left: Int, face: BufferedImage) {
        for (x in 0 until FACE * ZOOM) {
            for (y in 0 until FACE * ZOOM) {
                image.setRGB(left + INSET + x, INSET + y, face.getRGB(x / ZOOM, y / ZOOM))
            }
        }
    }

    private fun plus(image: BufferedImage, left: Int) {
        val center = SLOT / 2
        for (d in -PLUS_ARM..PLUS_ARM) {
            image.setRGB(left + center + d, center, PLUS)
            image.setRGB(left + center, center + d, PLUS)
        }
    }

    private const val OPAQUE = 128
    private const val OPAQUE_MASK = 0xFF000000.toInt()
}
