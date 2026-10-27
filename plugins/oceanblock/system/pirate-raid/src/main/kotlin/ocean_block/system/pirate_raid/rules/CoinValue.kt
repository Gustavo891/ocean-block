package ocean_block.system.pirate_raid.rules

import kotlin.math.roundToInt
import kotlin.random.Random

object CoinValue {

    private const val BASE_LEVEL = 10

    fun factor(level: Int, levelFactor: Double): Double =
        1.0 + levelFactor * (level - BASE_LEVEL).coerceAtLeast(0)

    fun coin(min: Int, max: Int, level: Int, levelFactor: Double, random: Random): Int =
        (random.nextInt(min, max + 1) * factor(level, levelFactor)).roundToInt()

    fun victory(base: Int, level: Int, levelFactor: Double): Int =
        (base * factor(level, levelFactor)).roundToInt()
}
