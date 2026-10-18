package ocean_block.system.drift.net

object NetRules {

    fun nearest(debrisX: Double, debrisY: Double, debrisZ: Double, nets: List<NetPoint>, radius: Double): NetPoint? {
        val radiusSquared = radius * radius
        return nets.asSequence()
            .filterNot { it.full }
            .map { it to distanceSquared(debrisX, debrisY, debrisZ, it) }
            .filter { (_, distance) -> distance <= radiusSquared }
            .minByOrNull { (_, distance) -> distance }
            ?.first
    }

    fun isFull(slots: List<NetSlot>, capacity: Int): Boolean =
        slots.size >= capacity && slots.all { it.amount >= it.maxStack }

    private fun distanceSquared(x: Double, y: Double, z: Double, net: NetPoint): Double {
        val dx = net.x - x
        val dy = net.y - y
        val dz = net.z - z
        return dx * dx + dy * dy + dz * dz
    }
}
