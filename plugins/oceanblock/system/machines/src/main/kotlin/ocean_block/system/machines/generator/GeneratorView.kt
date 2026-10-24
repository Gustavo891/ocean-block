package ocean_block.system.machines.generator

data class FuelStack(val material: String, val amount: Int)

object GeneratorView {

    private const val TICKS_PER_SECOND = 20
    private const val BATTERY_SEGMENTS = 10

    fun batteryLevel(energy: Long, capacity: Long): Int =
        ((energy * BATTERY_SEGMENTS + capacity - 1) / capacity).toInt().coerceIn(0, BATTERY_SEGMENTS)

    fun percent(energy: Long, capacity: Long): Int = (energy * 100 / capacity).toInt().coerceIn(0, 100)

    fun burnSeconds(state: GeneratorState, fuelTicks: (String) -> Int?): Int {
        val stored = state.slots.filterNotNull().sumOf { stack -> (fuelTicks(stack.material) ?: 0) * stack.amount }
        return (state.burnTicks + stored) / TICKS_PER_SECOND
    }
}
