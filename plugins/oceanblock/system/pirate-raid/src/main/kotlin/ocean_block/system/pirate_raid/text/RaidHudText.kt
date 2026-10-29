package ocean_block.system.pirate_raid.text

import kotlin.math.ceil

object RaidHudText {

    const val BACKGROUND = "hud_invasao"

    private const val WAVE_DONE = "<glyph:hud_pirata_onda_feita>"
    private const val WAVE_NEXT = "<glyph:hud_pirata_onda_vazia>"
    private const val SWORDS = "<glyph:hud_pirata_espadas>"
    private const val HOURGLASS = "<glyph:hud_pirata_ampulheta>"
    private const val GAP = "   "

    fun line(wave: Int, waves: Int, alive: Int, secondsLeft: Int): String {
        val seconds = secondsLeft.coerceAtLeast(0)
        val clock = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
        val dots = (1..waves).joinToString(" ") { if (it <= wave) WAVE_DONE else WAVE_NEXT }
        return "<red>Invasão</red>$GAP<white>$dots</white>$GAP<white>$SWORDS $alive</white>$GAP<white>$HOURGLASS</white> <yellow>$clock</yellow>"
    }

    fun health(current: Double, max: Double, gold: Boolean): String {
        val color = if (gold) "gold" else "red"
        val now = ceil(current.coerceAtLeast(0.0)).toInt()
        val full = ceil(max).toInt()
        return "<$color>❤</$color> <white>$now/$full</white>"
    }
}
