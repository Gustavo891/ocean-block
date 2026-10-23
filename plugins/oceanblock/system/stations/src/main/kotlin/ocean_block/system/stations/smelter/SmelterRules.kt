package ocean_block.system.stations.smelter

import kotlin.random.Random

object SmelterRules {

    const val MAX_INPUT = 32
    const val MAX_FUEL = 16
    const val MAX_OUTPUT = 16

    private val INGOTS = mapOf(
        "RAW_IRON" to "IRON_INGOT",
        "RAW_COPPER" to "COPPER_INGOT",
        "RAW_GOLD" to "GOLD_INGOT",
    )

    fun accepts(id: String): Boolean = id in INGOTS

    fun ingotOf(id: String): String = INGOTS.getValue(id)

    fun insertOre(state: SmelterState, id: String, amount: Int): Pair<SmelterState, Int> {
        if (!accepts(id) || (state.input != null && state.input != id)) return state to 0
        val taken = minOf(amount, MAX_INPUT - state.inputCount).coerceAtLeast(0)
        if (taken == 0) return state to 0
        return state.copy(input = id, inputCount = state.inputCount + taken) to taken
    }

    fun insertFuel(state: SmelterState, material: String, amount: Int): Pair<SmelterState, Int> {
        if (Fuel.ticks(material) == null || (state.fuelItem != null && state.fuelItem != material)) return state to 0
        val taken = minOf(amount, MAX_FUEL - state.fuelCount).coerceAtLeast(0)
        if (taken == 0) return state to 0
        return state.copy(fuelItem = material, fuelCount = state.fuelCount + taken) to taken
    }

    fun tick(state: SmelterState, tier: SmelterTier, ticks: Int, random: Random): SmelterState {
        var current = state
        var remaining = ticks
        while (remaining > 0 && canSmelt(current)) {
            if (current.burnTicks == 0) current = ignite(current) ?: break
            val step = minOf(remaining, current.burnTicks, tier.ticksPerIngot - current.progressTicks)
            remaining -= step
            current = current.copy(burnTicks = current.burnTicks - step, progressTicks = current.progressTicks + step)
            if (current.progressTicks >= tier.ticksPerIngot) current = finish(current, tier, random)
        }
        return current
    }

    fun takeOutput(state: SmelterState): Pair<SmelterState, Pair<String, Int>?> {
        val output = state.output ?: return state to null
        return state.copy(output = null, outputCount = 0) to (output to state.outputCount)
    }

    fun takeAll(state: SmelterState): Pair<SmelterState, List<Pair<String, Int>>> {
        val items = listOfNotNull(
            state.output?.let { it to state.outputCount },
            state.input?.let { it to state.inputCount },
            state.fuelItem?.let { it to state.fuelCount },
        )
        return SmelterState.EMPTY to items
    }

    private fun canSmelt(state: SmelterState): Boolean {
        val input = state.input ?: return false
        if (state.inputCount < 1) return false
        val ingot = ingotOf(input)
        return (state.output == null || state.output == ingot) && state.outputCount < MAX_OUTPUT
    }

    private fun ignite(state: SmelterState): SmelterState? {
        val fuel = state.fuelItem?.takeIf { state.fuelCount > 0 } ?: return null
        val left = state.fuelCount - 1
        return state.copy(burnTicks = Fuel.ticks(fuel) ?: 0, fuelCount = left, fuelItem = fuel.takeIf { left > 0 })
    }

    private fun finish(state: SmelterState, tier: SmelterTier, random: Random): SmelterState {
        val input = checkNotNull(state.input)
        val left = state.inputCount - 1
        val made = 1 + if (random.nextDouble() < tier.bonusChance) 1 else 0
        return state.copy(
            input = input.takeIf { left > 0 },
            inputCount = left,
            progressTicks = 0,
            output = ingotOf(input),
            outputCount = minOf(MAX_OUTPUT, state.outputCount + made),
        )
    }
}
