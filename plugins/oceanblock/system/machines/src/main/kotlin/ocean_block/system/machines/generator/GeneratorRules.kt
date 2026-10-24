package ocean_block.system.machines.generator

object GeneratorRules {

    const val CAPACITY = 10_000L
    const val RATE = 2
    const val MAX_STACK = 64

    fun insertFuel(state: GeneratorState, material: String, amount: Int, fuelTicks: (String) -> Int?): Pair<GeneratorState, Int> {
        if (fuelTicks(material) == null || amount <= 0) return state to 0
        val slots = state.slots.toMutableList()
        var left = amount
        val order = slots.indices.filter { slots[it]?.material == material } + slots.indices.filter { slots[it] == null }
        order.forEach { index ->
            if (left == 0) return@forEach
            val current = slots[index]?.amount ?: 0
            val add = minOf(left, MAX_STACK - current)
            if (add <= 0) return@forEach
            slots[index] = FuelStack(material, current + add)
            left -= add
        }
        val taken = amount - left
        return if (taken == 0) state to 0 else state.copy(slots = slots.toList()) to taken
    }

    fun takeSlot(state: GeneratorState, index: Int): Pair<GeneratorState, FuelStack?> {
        val stack = state.slots.getOrNull(index) ?: return state to null
        return state.copy(slots = state.slots.mapIndexed { i, current -> current.takeIf { i != index } }) to stack
    }

    fun takeFuel(state: GeneratorState): Pair<GeneratorState, List<FuelStack>> =
        state.copy(slots = List(GeneratorState.SLOTS) { null }) to state.slots.filterNotNull()

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
        val index = state.slots.indexOfFirst { it != null && fuelTicks(it.material) != null }
        if (index < 0) return null
        val stack = state.slots[index] ?: return null
        val burn = fuelTicks(stack.material) ?: return null
        val slots = state.slots.toMutableList().also { it[index] = stack.copy(amount = stack.amount - 1).takeIf { left -> left.amount > 0 } }
        return state.copy(slots = slots.toList(), burnTicks = burn)
    }
}
