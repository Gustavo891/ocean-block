package ocean_block.system.machines.energy

object EnergyShapes {

    private const val EPSILON = 1e-9

    fun trail(length: Double, step: Double, phase: Double): List<Double> =
        generateSequence(phase) { it + step }.takeWhile { it <= length + EPSILON }.toList()

    fun phase(tick: Int, cycle: Int, step: Double): Double = Math.floorMod(tick, cycle).toDouble() / cycle * step

    fun circle(radius: Double, count: Int): List<Pair<Double, Double>> = (0 until count).map { index ->
        val angle = 2 * Math.PI * index / count
        radius * Math.cos(angle) to radius * Math.sin(angle)
    }
}
