package ocean_block.system.machines.energy

import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

data class BeamPoint(val x: Double, val y: Double, val z: Double) {
    fun distanceTo(other: BeamPoint): Double {
        val dx = other.x - x
        val dy = other.y - y
        val dz = other.z - z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}

object BeamPath {

    private const val MIN_SEGMENTS = 2
    private const val MAX_SEGMENTS = 12

    fun segmentsFor(distance: Double): Int = distance.roundToInt().coerceIn(MIN_SEGMENTS, MAX_SEGMENTS)

    fun points(from: BeamPoint, to: BeamPoint, segments: Int, jitter: Double, random: Random): List<BeamPoint> =
        (0..segments).map { index ->
            val t = index.toDouble() / segments
            val straight = BeamPoint(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t, from.z + (to.z - from.z) * t)
            if (index == 0 || index == segments || jitter == 0.0) {
                if (index == 0) from else if (index == segments) to else straight
            } else {
                BeamPoint(straight.x + offset(random, jitter), straight.y + offset(random, jitter), straight.z + offset(random, jitter))
            }
        }

    private fun offset(random: Random, jitter: Double): Double = (random.nextDouble() * 2 - 1) * jitter
}
