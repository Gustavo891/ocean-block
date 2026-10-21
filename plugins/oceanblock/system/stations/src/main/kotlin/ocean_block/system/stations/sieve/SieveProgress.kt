package ocean_block.system.stations.sieve

data class SieveState(val input: SieveInput?, val stepsLeft: Int)

sealed interface SieveStep {
    data object Empty : SieveStep
    data class Advanced(val state: SieveState) : SieveStep
    data class Done(val input: SieveInput) : SieveStep
}

object SieveProgress {

    const val COOLDOWN_TICKS = 4L
    private const val FULL_LEVEL = 6
    private const val INPUT = "sieve_input"
    private const val STEPS = "sieve_steps"

    val EMPTY = SieveState(null, 0)

    fun load(state: SieveState, input: SieveInput, tier: SieveTier): SieveState? =
        if (state.input != null) null else SieveState(input, tier.steps)

    fun step(state: SieveState): SieveStep {
        val input = state.input ?: return SieveStep.Empty
        return if (state.stepsLeft <= 1) SieveStep.Done(input) else SieveStep.Advanced(state.copy(stepsLeft = state.stepsLeft - 1))
    }

    fun composterLevel(state: SieveState, tier: SieveTier): Int {
        if (state.input == null) return 0
        if (tier.steps <= 1) return FULL_LEVEL
        val left = state.stepsLeft.coerceIn(1, tier.steps)
        return 1 + (left - 1) * (FULL_LEVEL - 1) / (tier.steps - 1)
    }

    fun allowed(lastTick: Long?, now: Long): Boolean = lastTick == null || now - lastTick >= COOLDOWN_TICKS

    fun toCounters(state: SieveState): Map<String, Long> =
        mapOf(INPUT to ((state.input?.ordinal ?: -1) + 1).toLong(), STEPS to state.stepsLeft.toLong())

    fun fromCounters(counters: Map<String, Long>): SieveState {
        val input = counters[INPUT]?.toInt()?.let { SieveInput.entries.getOrNull(it - 1) } ?: return EMPTY
        return SieveState(input, counters[STEPS]?.toInt() ?: 0)
    }
}
