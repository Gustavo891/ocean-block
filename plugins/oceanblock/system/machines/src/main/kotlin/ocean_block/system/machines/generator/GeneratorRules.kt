package ocean_block.system.machines.generator

object GeneratorRules {

    const val CAPACITY = 10_000L
    const val RATE = 2
    const val MAX_FUEL = 64

    fun insertFuel(state: GeneratorState, material: String, amount: Int, fuelTicks: (String) -> Int?): Pair<GeneratorState, Int> {
        if (fuelTicks(material) == null || (state.fuel != null && state.fuel != material)) return state to 0
        val taken = minOf(amount, MAX_FUEL - state.fuelCount).coerceAtLeast(0)
        if (taken == 0) return state to 0
        return state.copy(fuel = material, fuelCount = state.fuelCount + taken) to taken
    }

    fun takeFuel(state: GeneratorState): Pair<GeneratorState, Pair<String, Int>?> {
        val fuel = state.fuel ?: return state to null
        return state.copy(fuel = null, fuelCount = 0) to (fuel to state.fuelCount)
    }

    fun tick(state: GeneratorState, ticks: Int, fuelTicks: (String) -> Int?): GeneratorState {
        var current = state
        var remaining = ticks
        while (remaining > 0 && current.energy < CAPACITY) {
            if (current.burnTicks == 0) current = ignite(current, fuelTicks) ?: break
            val room = (CAPACITY - current.energy + RATE - 1) / RATE
            val step = minOf(remaining.toLong(), current.burnTicks.toLong(), room).toInt()
            remaining -= step
            current = current.copy(
                burnTicks = current.burnTicks - step,
                energy = minOf(CAPACITY, current.energy + step.toLong() * RATE),
            )
        }
        return current
    }

    private fun ignite(state: GeneratorState, fuelTicks: (String) -> Int?): GeneratorState? {
        val fuel = state.fuel?.takeIf { state.fuelCount > 0 } ?: return null
        val burn = fuelTicks(fuel) ?: return null
        val left = state.fuelCount - 1
        return state.copy(fuel = fuel.takeIf { left > 0 }, fuelCount = left, burnTicks = burn)
    }
}
