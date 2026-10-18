package one_block.system.spawners.rules

object PlacementRules {

    const val MIN_DISTANCE = 5.0

    fun isTooClose(
        ax: Double, ay: Double, az: Double,
        bx: Double, by: Double, bz: Double,
        minDistance: Double = MIN_DISTANCE,
    ): Boolean {
        val dx = ax - bx
        val dy = ay - by
        val dz = az - bz
        return (dx * dx + dy * dy + dz * dz) < (minDistance * minDistance)
    }
}
