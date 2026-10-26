package ocean_block.system.islands.dialog

import java.awt.image.BufferedImage

object MemberSlots {

    const val SLOT = 18

    private const val FACE = 8
    private const val ZOOM = 2
    private const val INSET = 1

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

    fun row(faces: List<BufferedImage?>, capacity: Int, total: Int): BufferedImage {
        val image = BufferedImage(total * SLOT, SLOT, BufferedImage.TYPE_INT_ARGB)
        val start = (total - capacity) * SLOT / 2
        faces.take(capacity).forEachIndexed { index, face ->
            if (face == null) return@forEachIndexed
            for (x in 0 until FACE * ZOOM) {
                for (y in 0 until FACE * ZOOM) {
                    image.setRGB(start + index * SLOT + INSET + x, INSET + y, face.getRGB(x / ZOOM, y / ZOOM))
                }
            }
        }
        return image
    }

    private const val OPAQUE = 128
    private const val OPAQUE_MASK = 0xFF000000.toInt()
}
