package ocean_block.system.drift.debris

import org.bukkit.Material

object DebrisRules {

    const val TICKS_PER_STEP = 2
    const val PULL_TICKS = 10

    private const val MILLIS_PER_SECOND = 1000L

    fun isPassable(material: Material): Boolean = !material.isSolid

    fun stepDistance(speedPerTick: Double): Double = speedPerTick * TICKS_PER_STEP

    fun stuckExpired(stuckSinceMillis: Long?, nowMillis: Long, stuckSeconds: Int): Boolean =
        stuckSinceMillis != null && nowMillis - stuckSinceMillis >= stuckSeconds * MILLIS_PER_SECOND

    fun pullFraction(ticksLeft: Int): Double =
        if (ticksLeft <= TICKS_PER_STEP) 1.0 else TICKS_PER_STEP.toDouble() / ticksLeft
}
