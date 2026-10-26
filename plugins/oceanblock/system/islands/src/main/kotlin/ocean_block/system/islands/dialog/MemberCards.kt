package ocean_block.system.islands.dialog

import java.awt.image.BufferedImage

object MemberCards {

    class Card(val portrait: BufferedImage?, val owner: Boolean, val online: Boolean)

    const val SLOTS = 6
    const val CARD_WIDTH = 42
    const val CARD_HEIGHT = 48
    const val GAP = 4
    const val PORTRAIT_WIDTH = 30
    const val PORTRAIT_HEIGHT = 30
    const val PORTRAIT_Y = 6
    const val CROWN_X = 3
    const val CROWN_Y = 3
    const val PLUS_Y = 16

    const val FILL = 0xFF46494F.toInt()
    const val BORDER = 0xFF6A6D72.toInt()
    const val CROWN = 0xFFF2B600.toInt()
    const val ONLINE = 0xFF4ADE5A.toInt()
    const val OFFLINE = 0xFF6A6D72.toInt()
    const val PLUS = 0xFF8E9196.toInt()

    private const val PLUS_ARM = 5
    private val CROWN_SHAPE = listOf("#..#..#", "##.#.##", "#######", "#######")

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
        card.portrait?.let { portrait ->
            val ox = left + (CARD_WIDTH - portrait.width) / 2
            for (x in 0 until portrait.width) {
                for (y in 0 until portrait.height) {
                    val argb = portrait.getRGB(x, y)
                    if ((argb ushr 24) != 0) image.setRGB(ox + x, PORTRAIT_Y + y, argb)
                }
            }
        }
        if (card.owner) {
            CROWN_SHAPE.forEachIndexed { row, line ->
                line.forEachIndexed { column, char -> if (char == '#') image.setRGB(left + CROWN_X + column, CROWN_Y + row, CROWN) }
            }
        }
        val dot = if (card.online) ONLINE else OFFLINE
        for (x in CARD_WIDTH - 5 until CARD_WIDTH - 2) for (y in 2 until 5) image.setRGB(left + x, y, dot)
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
