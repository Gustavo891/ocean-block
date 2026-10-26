package ocean_block.system.level.levelup

import java.text.NumberFormat
import java.util.Locale

object LevelUpCard {

    const val PADDING = 22
    private const val RIGHT_PADDING = 8
    val WIDTHS: List<Int> = listOf(110, 140, 170, 200, 240)
    private val LOCALE: Locale = Locale.forLanguageTag("pt-BR")

    fun lines(level: Int, coins: Double, unlocked: List<String>): List<String> {
        val texts = listOfNotNull(
            "aqua" to "Nível $level!",
            coins.takeIf { it > 0 }?.let { "yellow" to "+${number(it)} Coins" },
            unlocked.takeIf { it.isNotEmpty() }?.let { "green" to "Liberado: ${it.joinToString(", ")}" },
        )
        val width = width(texts.map { it.second })
        val rows = if (texts.size >= 3) 3 else 2
        return texts.mapIndexed { index, (color, text) ->
            val lead = if (index == 0) "<shadow:#00000000><white><glyph:nivel_cartao_${rows}_$width></white></shadow><shift:-${width - PADDING}>"
            else "<shift:$PADDING>"
            "$lead<$color>$text</$color><shift:${width - PADDING - RewardText.width(text)}>"
        }
    }

    fun width(texts: List<String>): Int {
        val needed = PADDING + (texts.maxOfOrNull(RewardText::width) ?: 0) + RIGHT_PADDING
        return WIDTHS.firstOrNull { it >= needed } ?: WIDTHS.last()
    }

    fun glyphIds(): List<String> = WIDTHS.flatMap { w -> listOf("nivel_cartao_2_$w", "nivel_cartao_3_$w") }

    private fun number(value: Double): String = NumberFormat.getIntegerInstance(LOCALE).format(value.toLong())
}
