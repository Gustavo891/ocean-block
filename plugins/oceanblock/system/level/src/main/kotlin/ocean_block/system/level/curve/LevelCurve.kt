package ocean_block.system.level.curve

import ocean_block.system.scoreboard.level.LevelProgress
import kotlin.math.pow

object LevelCurve {

    const val MAX_LEVEL = 50

    private val totals: LongArray = LongArray(MAX_LEVEL + 1).also { totals ->
        for (level in 2..MAX_LEVEL) totals[level] = totals[level - 1] + toNext(level - 1)
    }

    fun toNext(level: Int): Long = Math.round(100.0 * level.toDouble().pow(1.5))

    fun totalFor(level: Int): Long = totals[level.coerceIn(1, MAX_LEVEL)]

    fun levelOf(xp: Long): Int = (MAX_LEVEL downTo 1).first { xp >= totals[it] || it == 1 }

    fun progress(xp: Long): LevelProgress {
        val level = levelOf(xp)
        if (level >= MAX_LEVEL) return LevelProgress(MAX_LEVEL, 1.0)
        return LevelProgress(level, (xp - totals[level]).toDouble() / toNext(level))
    }
}
