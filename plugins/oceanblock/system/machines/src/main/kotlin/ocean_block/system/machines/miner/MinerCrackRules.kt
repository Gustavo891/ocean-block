package ocean_block.system.machines.miner

object MinerCrackRules {

    const val STAGES = 10
    const val CYCLE_TICKS = 10

    fun progress(paid: Int, sinceTicks: Int): Int =
        minOf(paid + sinceTicks.coerceIn(0, CYCLE_TICKS), MinerRules.INTERVAL)

    fun stage(progress: Int): Int = (progress * STAGES / MinerRules.INTERVAL).coerceIn(0, STAGES - 1)

    fun damage(stage: Int): Float = (stage + 1).toFloat() / STAGES
}
