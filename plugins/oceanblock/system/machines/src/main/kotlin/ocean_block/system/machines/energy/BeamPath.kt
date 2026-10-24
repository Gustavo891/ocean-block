package ocean_block.system.machines.energy

import kotlin.math.roundToInt
import kotlin.math.sqrt

data class BeamPoint(val x: Double, val y: Double, val z: Double) {
    fun distanceTo(other: BeamPoint): Double {
        val dx = other.x - x
        val dy = other.y - y
        val dz = other.z - z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}

object BeamPath {

    fun line(from: BeamPoint, to: BeamPoint, spacing: Double): List<BeamPoint> {
        val steps = maxOf(1, (from.distanceTo(to) / spacing).roundToInt())
        return (0..steps).map { step ->
            val t = step.toDouble() / steps
            BeamPoint(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t, from.z + (to.z - from.z) * t)
        }
    }
}
