package ocean_block.system.drift.debris

import kotlin.math.abs
import kotlin.math.floor

sealed interface Passage {
    data object Unloaded : Passage
    data object Unsupported : Passage
    data object Drop : Passage
    data object Blocked : Passage
    data class Open(val depth: Int, val atSurface: Boolean) : Passage
}

object DiveRules {

    const val ARRIVAL_TOLERANCE = 0.05
    const val SUBMERGED_BOB_SCALE = 0.3
    const val FADE_SINK = 0.5
    const val SCAN_MARGIN = 4

    private const val BLOCK_CENTER = 0.5

    fun passage(
        loaded: Boolean,
        depth: Int,
        levelDepth: Int,
        maxDepth: Int,
        here: (Int) -> BlockKind,
        front: (Int) -> BlockKind,
    ): Passage {
        if (!loaded) return Passage.Unloaded
        if (here(depth) != BlockKind.WATER) return Passage.Unsupported
        if (front(depth) == BlockKind.AIR && front(levelDepth) == BlockKind.AIR) return Passage.Drop
        return open(depth, maxDepth, here, front) ?: Passage.Blocked
    }

    private fun open(depth: Int, maxDepth: Int, here: (Int) -> BlockKind, front: (Int) -> BlockKind): Passage.Open? {
        val scan = maxDepth + SCAN_MARGIN
        val limit = minOf(maxOf(maxDepth, depth), scan)
        val frontWater = { at: Int -> front(at) == BlockKind.WATER }
        val target = targetDepth(depth, limit, frontWater) { here(it) == BlockKind.WATER } ?: return null
        val surface = GravityRules.surfaceDepth(scan, frontWater) { front(it) == BlockKind.SOLID }
        return Passage.Open(target, surface == target)
    }

    fun targetDepth(
        currentDepth: Int,
        maxDepth: Int,
        aheadPassable: (Int) -> Boolean,
        columnPassable: (Int) -> Boolean,
    ): Int? =
        (0..maxDepth).firstOrNull { depth ->
            aheadPassable(depth) && between(currentDepth, depth).all(columnPassable)
        }

    fun stepHeight(currentY: Double, targetY: Double, speedPerStep: Double): Double {
        val distance = targetY - currentY
        val speed = speedPerStep.coerceAtLeast(0.0)
        return if (abs(distance) <= speed) targetY else currentY + speed * Math.signum(distance)
    }

    fun atTarget(currentY: Double, targetY: Double): Boolean = abs(currentY - targetY) <= ARRIVAL_TOLERANCE

    fun fadeProgress(ticksIntoFade: Int, fadeTicks: Int): Double =
        if (fadeTicks <= 0) 1.0 else (ticksIntoFade.toDouble() / fadeTicks).coerceIn(0.0, 1.0)

    fun levelY(depth: Int, surfaceY: Double, sink: Double): Double = levelY(depth, depth <= 0, surfaceY, sink)

    fun levelY(depth: Int, atSurface: Boolean, surfaceY: Double, sink: Double): Double =
        if (atSurface) surfaceY - depth - sink else floor(surfaceY) - depth + BLOCK_CENTER

    fun bobScale(currentY: Double, surfaceLevelY: Double): Double =
        if (atTarget(currentY, surfaceLevelY)) 1.0 else SUBMERGED_BOB_SCALE

    fun raftY(surfaceY: Double): Int = floor(surfaceY).toInt()

    private fun between(from: Int, to: Int): IntProgression =
        when {
            to > from -> (from + 1)..to
            to < from -> (from - 1) downTo to
            else -> IntRange.EMPTY
        }
}
