package ocean_block.system.machines.solar

object SolarRules {

    const val CAPACITY = 1_000L
    const val PER_TEN_TICKS = 3

    private const val DAY_LENGTH = 24_000L
    private const val DAY_END = 12_000L

    fun isDay(time: Long): Boolean = Math.floorMod(time, DAY_LENGTH) < DAY_END

    fun produce(energy: Long, ticks: Int, active: Boolean): Long {
        if (!active) return energy
        return minOf(CAPACITY, energy + PER_TEN_TICKS.toLong() * ticks / 10)
    }
}
