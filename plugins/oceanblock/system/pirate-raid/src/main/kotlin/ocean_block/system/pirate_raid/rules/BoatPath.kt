package ocean_block.system.pirate_raid.rules

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

data class Vec(val x: Double, val z: Double)

object BoatPath {

    const val STILL_LIMIT = 60

    fun spawnPoint(center: Vec, radius: Double, angle: Double): Vec =
        Vec(center.x + cos(angle) * radius, center.z + sin(angle) * radius)

    fun step(position: Vec, center: Vec, speed: Double): Vec {
        val dx = center.x - position.x
        val dz = center.z - position.z
        val distance = hypot(dx, dz)
        if (distance == 0.0) return Vec(0.0, 0.0)
        val length = min(speed, distance)
        return Vec(dx / distance * length, dz / distance * length)
    }

    fun distance(position: Vec, center: Vec): Double = hypot(center.x - position.x, center.z - position.z)

    fun landed(distance: Double, landingDistance: Double, blockedAhead: Boolean, stillTicks: Int): Boolean =
        distance <= landingDistance || blockedAhead || stillTicks >= STILL_LIMIT
}
