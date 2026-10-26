package ocean_block.system.level.levelup

import java.text.NumberFormat
import java.util.Locale

object LevelUpCard {

    private const val WIDTH = 160
    private const val TEXT_X = 30
    private val LOCALE: Locale = Locale.forLanguageTag("pt-BR")

    fun lines(level: Int, coins: Double): List<String> {
        val room = WIDTH - TEXT_X
        val title = "Nível $level!"
        val first = "<shadow:#00000000><white><glyph:toast_nivel></white><shift:-$room>" +
            "<yellow>$title</yellow><shift:${room - RewardText.width(title)}></shadow>"
        if (coins <= 0) return listOf(first)
        val reward = "+${NumberFormat.getIntegerInstance(LOCALE).format(coins.toLong())} Coins"
        return listOf(first, "<shadow:#00000000><shift:$TEXT_X><white>$reward</white><shift:${room - RewardText.width(reward)}></shadow>")
    }
}
