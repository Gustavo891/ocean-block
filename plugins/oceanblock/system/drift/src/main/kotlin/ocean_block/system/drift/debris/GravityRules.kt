package ocean_block.system.drift.debris

import kotlin.math.floor

enum class BlockKind { SOLID, WATER, AIR }

enum class Support { FLOAT, FALL, GROUND }

sealed interface Fall {
    data object Airborne : Fall
    data class Splash(val blockY: Int) : Fall
    data class Landed(val topY: Int) : Fall
}

object GravityRules {

    const val GRAVITY = 0.04
    const val DRAG = 0.98
    const val TERMINAL_VELOCITY = -1.0
    const val GROUND_LIFT = 0.25
    const val MAX_FALL_BELOW_RAFT = 64

    fun fall(vy: Double, ticks: Int): Double = (0 until ticks).fold(vy) { velocity, _ -> tick(velocity) }

    fun descent(vy: Double, ticks: Int): Double {
        var velocity = vy
        var distance = 0.0
        repeat(ticks) {
            velocity = tick(velocity)
            distance += velocity
        }
        return distance
    }

    fun surfaceDepth(maxScan: Int, isWater: (Int) -> Boolean, isSolid: (Int) -> Boolean): Int? {
        for (depth in 0..maxScan) {
            if (isSolid(depth)) return null
            if (isWater(depth)) return depth
        }
        return null
    }

    fun support(waterHere: Boolean, solidBelow: Boolean): Support =
        when {
            waterHere -> Support.FLOAT
            solidBelow -> Support.GROUND
            else -> Support.FALL
        }

    fun restingSupport(landedTopY: Int, isWater: (Int) -> Boolean, isSolid: (Int) -> Boolean): Support =
        support(isWater(landedTopY), isSolid(landedTopY - 1))

    fun settle(fromY: Double, toY: Double, isWater: (Int) -> Boolean, isSolid: (Int) -> Boolean): Fall {
        for (blockY in floor(fromY).toInt() downTo floor(toY).toInt()) {
            if (isSolid(blockY)) return Fall.Landed(blockY + 1)
            if (isWater(blockY)) return Fall.Splash(blockY)
        }
        return Fall.Airborne
    }

    fun restY(topY: Int, scale: Double): Double = topY + GROUND_LIFT * scale

    fun outOfWorld(y: Double, raftY: Int, minHeight: Int): Boolean =
        y < minHeight || y < raftY - MAX_FALL_BELOW_RAFT

    private fun tick(velocity: Double): Double = ((velocity - GRAVITY) * DRAG).coerceAtLeast(TERMINAL_VELOCITY)
}
