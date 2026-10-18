package one_block.textura.text

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component

object SpacingFont {

    private val PLUS_4 = Char(0xE997)
    private val PLUS_1 = Char(0xE998)
    private val MINUS_1 = Char(0xE995)
    private val MINUS_4 = Char(0xE996)
    private val MINUS_8 = Char(0xE999)
    private val MINUS_16 = Char(0xE994)
    private const val MAX_SHIFT = 4096

    val KEY: Key = Key.key("minecraft", "spacing")

    val ADVANCES: Map<String, Int> = linkedMapOf(
        " " to 4,
        PLUS_4.toString() to 4,
        PLUS_1.toString() to 1,
        MINUS_1.toString() to -1,
        MINUS_4.toString() to -4,
        MINUS_8.toString() to -8,
        MINUS_16.toString() to -16,
    )

    fun chars(pixels: Int): String {
        val builder = StringBuilder()
        var remaining = pixels.coerceIn(-MAX_SHIFT, MAX_SHIFT)

        while (remaining > 0) {
            if (remaining >= 4) {
                builder.append(PLUS_4)
                remaining -= 4
            } else {
                builder.append(PLUS_1)
                remaining -= 1
            }
        }

        while (remaining < 0) {
            when {
                remaining <= -16 -> builder.append(MINUS_16).also { remaining += 16 }
                remaining <= -8 -> builder.append(MINUS_8).also { remaining += 8 }
                remaining <= -4 -> builder.append(MINUS_4).also { remaining += 4 }
                else -> builder.append(MINUS_1).also { remaining += 1 }
            }
        }
        return builder.toString()
    }

    fun component(pixels: Int): Component = Component.text(chars(pixels)).font(KEY)
}
