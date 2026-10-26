package ocean_block.system.level.levelup

import java.text.NumberFormat
import java.util.Locale

object LevelUpScreen {

    const val FRAMES = 3
    private val LOCALE: Locale = Locale.forLanguageTag("pt-BR")

    fun title(): String = "<white><glyph:nivel_faixa>"

    fun subtitle(level: Int, frame: Int): String =
        "<white><glyph:nivel_brilho_e_$frame>" + level.toString().map { "<glyph:nivel_num_$it>" }.joinToString("") +
            "<glyph:nivel_brilho_d_$frame>"

    fun rewardLines(coins: Double, unlocked: List<String>): List<String> = listOfNotNull(
        coins.takeIf { it > 0 }?.let { centered("gold", "+${number(it)} Coins") },
        unlocked.takeIf { it.isNotEmpty() }?.let { centered("green", "Liberado: ${it.joinToString(", ")}") },
    )

    private fun centered(color: String, text: String): String = "<shift:-${RewardText.width(text) / 2}><$color>$text"

    private fun number(value: Double): String = NumberFormat.getIntegerInstance(LOCALE).format(value.toLong())
}
