package ocean_block.system.machines.generator

data class FuelStack(val material: String, val amount: Int)

object GeneratorView {

    private const val TICKS_PER_SECOND = 20

    fun burnSeconds(state: GeneratorState, fuelTicks: (String) -> Int?): Int {
        val stored = state.slots.filterNotNull().sumOf { stack -> (fuelTicks(stack.material) ?: 0) * stack.amount }
        return (state.burnTicks + stored) / TICKS_PER_SECOND
    }
}
