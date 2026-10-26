package ocean_block.system.playtime_rewards.notify

import ocean_block.system.playtime_rewards.text.RewardText

object RewardCard {

    const val HINT = "Resgate em /recompensas"

    private const val PADDING = 22
    private const val RIGHT_PADDING = 8
    private val WIDTHS: List<Int> = listOf(110, 140, 170, 200, 240)

    fun lines(minutes: Int): List<String> {
        val texts = listOf("aqua" to "Recompensa de ${RewardText.duration(minutes)}!", "green" to HINT)
        val width = width(texts.map { it.second })
        return texts.mapIndexed { index, (color, text) ->
            val lead = if (index == 0) "<shadow:#00000000><white><glyph:tempo_cartao_$width></white></shadow><shift:-${width - PADDING}>"
            else "<shift:$PADDING>"
            "$lead<$color>$text</$color><shift:${width - PADDING - LineText.width(text)}>"
        }
    }

    fun width(texts: List<String>): Int {
        val needed = PADDING + (texts.maxOfOrNull(LineText::width) ?: 0) + RIGHT_PADDING
        return WIDTHS.firstOrNull { it >= needed } ?: WIDTHS.last()
    }
}
