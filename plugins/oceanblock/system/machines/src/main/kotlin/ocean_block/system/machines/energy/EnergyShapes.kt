package ocean_block.system.machines.energy

object EnergyShapes {

    fun circle(radius: Double, count: Int): List<Pair<Double, Double>> = (0 until count).map { index ->
        val angle = 2 * Math.PI * index / count
        radius * Math.cos(angle) to radius * Math.sin(angle)
    }
}
