package ocean_block.system.machines.energy

import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
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

    private const val SEGMENTS_PER_BLOCK = 4
    private const val MIN_SEGMENTS = 6
    private const val MAX_SEGMENTS = 32

    fun segmentsFor(distance: Double): Int = (distance * SEGMENTS_PER_BLOCK).roundToInt().coerceIn(MIN_SEGMENTS, MAX_SEGMENTS)

    fun points(from: BeamPoint, to: BeamPoint, segments: Int, jitter: Double, random: Random): List<BeamPoint> =
        (0..segments).map { index ->
            val t = index.toDouble() / segments
            val straight = BeamPoint(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t, from.z + (to.z - from.z) * t)
            when (index) {
                0 -> from
                segments -> to
                else -> {
                    val amount = jitter * sin(PI * t)
                    BeamPoint(straight.x + offset(random, amount), straight.y + offset(random, amount), straight.z + offset(random, amount))
                }
            }
        }

    fun sample(points: List<BeamPoint>, spacing: Double): List<BeamPoint> {
        if (points.size < 2) return points
        val samples = mutableListOf(points.first())
        points.zipWithNext().forEach { (start, end) ->
            val steps = maxOf(1, (start.distanceTo(end) / spacing).roundToInt())
            (1..steps).forEach { step ->
                val t = step.toDouble() / steps
                samples += BeamPoint(start.x + (end.x - start.x) * t, start.y + (end.y - start.y) * t, start.z + (end.z - start.z) * t)
            }
        }
        return samples
    }

    private fun offset(random: Random, amount: Double): Double = (random.nextDouble() * 2 - 1) * amount
}
