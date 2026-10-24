package ocean_block.system.scoreboard.level

import java.util.Locale
import kotlin.math.floor

object LevelText {

    const val BAR_SEGMENTS = 10
    const val MAX_LEVEL = 9999

    private const val DIGIT_WIDTH = 6
    private const val PADDING = 2
    private val LOCALE = Locale.of("pt", "BR")

    fun badge(level: Int): String {
        val text = level.coerceIn(0, MAX_LEVEL).toString()
        val width = DIGIT_WIDTH * text.length - 1 + PADDING * 2
        return "<glyph:nivel_badge_${text.length}><shift:-${width - 1}><white>$text<shift:$PADDING>"
    }

    fun bar(progress: Double): String {
        val filled = floor(progress.coerceIn(0.0, 1.0) * BAR_SEGMENTS).toInt()
        return "<glyph:nivel_barra_$filled>"
    }

    fun percent(progress: Double): String =
        String.format(LOCALE, "%.2f%%", progress.coerceIn(0.0, 1.0) * 100)
}
