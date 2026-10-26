package ocean_block.system.islands.dialog

import java.awt.image.BufferedImage

object MemberCards {

    class Card(val face: BufferedImage?, val owner: Boolean, val online: Boolean)

    const val SLOTS = 6
    const val CARD_WIDTH = 18
    const val CARD_HEIGHT = 18
    const val GAP = 2
    const val FACE_X = 1
    const val FACE_Y = 1
    const val CROWN_X = 1
    const val CROWN_Y = 1
    const val DOT_X = 14
    const val DOT_Y = 1
    const val PLUS_Y = 9

    const val FILL = 0xFF46494F.toInt()
    const val BORDER = 0xFF6A6D72.toInt()
    const val CROWN = 0xFFF2B600.toInt()
    const val ONLINE = 0xFF4ADE5A.toInt()
    const val OFFLINE = 0xFF6A6D72.toInt()
    const val PLUS = 0xFF8E9196.toInt()

    private const val FACE = 8
    private const val ZOOM = 2
    private const val PLUS_ARM = 3
    private const val DOT = 3
    private const val OPAQUE = 128
    private const val OPAQUE_MASK = 0xFF000000.toInt()
    private val CROWN_SHAPE = listOf("#.#.#", "#####", "#####")

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

    fun render(cards: List<Card?>): BufferedImage {
        val image = BufferedImage(SLOTS * CARD_WIDTH + (SLOTS - 1) * GAP, CARD_HEIGHT, BufferedImage.TYPE_INT_ARGB)
        (0 until SLOTS).forEach { index ->
            val left = index * (CARD_WIDTH + GAP)
            val card = cards.getOrNull(index)
            if (card == null) plus(image, left) else filled(image, left, card)
        }
        return image
    }

    private fun filled(image: BufferedImage, left: Int, card: Card) {
        for (x in 0 until CARD_WIDTH) {
            for (y in 0 until CARD_HEIGHT) {
                val edge = x == 0 || y == 0 || x == CARD_WIDTH - 1 || y == CARD_HEIGHT - 1
                image.setRGB(left + x, y, if (edge) BORDER else FILL)
            }
        }
        card.face?.let { face ->
            for (x in 0 until FACE * ZOOM) for (y in 0 until FACE * ZOOM) image.setRGB(left + FACE_X + x, FACE_Y + y, face.getRGB(x / ZOOM, y / ZOOM))
        }
        if (card.owner) {
            CROWN_SHAPE.forEachIndexed { row, line ->
                line.forEachIndexed { column, char -> if (char == '#') image.setRGB(left + CROWN_X + column, CROWN_Y + row, CROWN) }
            }
        }
        val dot = if (card.online) ONLINE else OFFLINE
        for (x in DOT_X until DOT_X + DOT) for (y in DOT_Y until DOT_Y + DOT) image.setRGB(left + x, y, dot)
    }

    private fun plus(image: BufferedImage, left: Int) {
        val center = left + CARD_WIDTH / 2
        for (d in -PLUS_ARM..PLUS_ARM) {
            image.setRGB(center + d, PLUS_Y, PLUS)
            image.setRGB(center + d, PLUS_Y + 1, PLUS)
            image.setRGB(center, PLUS_Y + d, PLUS)
            image.setRGB(center - 1, PLUS_Y + d, PLUS)
        }
    }
}
