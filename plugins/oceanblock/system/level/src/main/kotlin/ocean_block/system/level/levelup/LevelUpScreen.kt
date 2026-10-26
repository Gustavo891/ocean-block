package ocean_block.system.level.levelup

import java.text.NumberFormat
import java.util.Locale

object LevelUpScreen {

    const val FRAMES = 3
    private const val GLYPH_ADVANCE = 6
    private val LOCALE: Locale = Locale.forLanguageTag("pt-BR")

    fun title(): String = "<white><glyph:nivel_faixa>"

    fun number(level: Int, frame: Int): String =
        "<glyph:nivel_brilho_e_$frame>" + level.toString().map { "<glyph:nivel_num_$it>" }.joinToString("") + "<glyph:nivel_brilho_d_$frame>"

    fun numberWidth(level: Int): Int = GLYPH_ADVANCE * (level.toString().length + 2)

    fun subtitle(level: Int, frame: Int, coins: Double, unlocked: List<String>): String {
        val width = numberWidth(level)
        val lines = listOfNotNull(
            coins.takeIf { it > 0 }?.let { Line("nivel_linha_1", "gold", "+${number(it)} Coins") },
            unlocked.takeIf { it.isNotEmpty() }?.let { Line("nivel_linha_2", "green", "Liberado: ${it.joinToString(", ")}") },
        )
        return "<white>" + number(level, frame) + lines.joinToString("") { it.render(width) }
    }

    private data class Line(val font: String, val color: String, val text: String) {

        fun render(numberWidth: Int): String {
            val width = SmallText.width(text)
            val back = (numberWidth + width) / 2
            return "<shift:-$back><font:oneblock:$font><$color>$text</$color></font><shift:${back - width}>"
        }
    }

    private fun number(value: Double): String = NumberFormat.getIntegerInstance(LOCALE).format(value.toLong())
}
