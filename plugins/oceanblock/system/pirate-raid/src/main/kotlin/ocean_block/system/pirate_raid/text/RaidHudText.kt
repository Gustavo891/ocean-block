package ocean_block.system.pirate_raid.text

import kotlin.math.ceil

object RaidHudText {

    private const val SEPARATOR = " <gray>-</gray> "

    fun line(wave: Int, waves: Int, alive: Int, secondsLeft: Int): String {
        val seconds = secondsLeft.coerceAtLeast(0)
        val clock = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
        val pirates = if (alive == 1) "1 pirata" else "$alive piratas"
        return listOf("<red>Invasão pirata</red>", "<white>Onda $wave/$waves</white>", "<white>$pirates</white>", "<yellow>$clock</yellow>")
            .joinToString(SEPARATOR)
    }

    fun health(current: Double, max: Double, gold: Boolean): String {
        val color = if (gold) "gold" else "red"
        val now = ceil(current.coerceAtLeast(0.0)).toInt()
        val full = ceil(max).toInt()
        return "<$color>❤</$color> <white>$now/$full</white>"
    }
}
