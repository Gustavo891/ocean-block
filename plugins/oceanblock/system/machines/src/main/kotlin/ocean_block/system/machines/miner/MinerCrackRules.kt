package ocean_block.system.machines.miner

object MinerCrackRules {

    const val STAGES = 10
    const val CYCLE_TICKS = DrillTier.CYCLE_TICKS

    fun progress(paid: Int, gain: Int, sinceTicks: Int): Int =
        minOf(paid + gain * sinceTicks.coerceIn(0, CYCLE_TICKS) / CYCLE_TICKS, MinerRules.BLOCK_UNITS)

    fun stage(progress: Int): Int = (progress * STAGES / MinerRules.BLOCK_UNITS).coerceIn(0, STAGES - 1)

    fun damage(stage: Int): Float = (stage + 1).toFloat() / STAGES
}
