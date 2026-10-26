package ocean_block.system.level.levelup

import java.text.NumberFormat
import java.util.Locale

data class ToastLine(val text: String, val icon: String)

object LevelUpCard {

    private const val BACKDROP = "glyph:toast_nivel"
    private const val LINE = "glyph:toast_linha"
    private val LOCALE: Locale = Locale.forLanguageTag("pt-BR")

    fun lines(level: Int, coins: Double): List<ToastLine> {
        val title = ToastLine("<yellow>Nível $level!</yellow>", BACKDROP)
        if (coins <= 0) return listOf(title)
        val reward = "+${NumberFormat.getIntegerInstance(LOCALE).format(coins.toLong())} Coins"
        return listOf(title, ToastLine("<white>$reward</white>", LINE))
    }
}
