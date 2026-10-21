package ocean_block.system.stations.smelter

data class SmelterState(
    val input: String?,
    val inputCount: Int,
    val fuelItem: String?,
    val fuelCount: Int,
    val burnTicks: Int,
    val progressTicks: Int,
    val output: String?,
    val outputCount: Int,
) {

    fun toCounters(): Map<String, Long> = buildMap {
        put(BURN, burnTicks.toLong())
        put(PROGRESS, progressTicks.toLong())
        input?.let { put(INPUT + it, inputCount.toLong()) }
        fuelItem?.let { put(FUEL + it, fuelCount.toLong()) }
        output?.let { put(OUTPUT + it, outputCount.toLong()) }
    }

    companion object {
        private const val BURN = "smelter_burn"
        private const val PROGRESS = "smelter_progress"
        private const val INPUT = "smelter_in/"
        private const val FUEL = "smelter_fuel/"
        private const val OUTPUT = "smelter_out/"

        val EMPTY = SmelterState(null, 0, null, 0, 0, 0, null, 0)

        val KEYS_PREFIX = "smelter_"

        fun fromCounters(counters: Map<String, Long>): SmelterState {
            fun slot(prefix: String): Pair<String, Int>? =
                counters.entries.firstOrNull { it.key.startsWith(prefix) && it.value > 0 }?.let { it.key.removePrefix(prefix) to it.value.toInt() }

            val input = slot(INPUT)
            val fuel = slot(FUEL)
            val output = slot(OUTPUT)
            return SmelterState(
                input = input?.first,
                inputCount = input?.second ?: 0,
                fuelItem = fuel?.first,
                fuelCount = fuel?.second ?: 0,
                burnTicks = counters[BURN]?.toInt() ?: 0,
                progressTicks = counters[PROGRESS]?.toInt() ?: 0,
                output = output?.first,
                outputCount = output?.second ?: 0,
            )
        }
    }
}
