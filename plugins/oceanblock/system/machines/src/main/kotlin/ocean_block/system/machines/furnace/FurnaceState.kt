package ocean_block.system.machines.furnace

data class FurnaceSlot(
    val input: String?,
    val inputCount: Int,
    val output: String?,
    val outputCount: Int,
    val progress: Int,
) {
    companion object {
        val EMPTY = FurnaceSlot(null, 0, null, 0, 0)
    }
}

data class FurnaceState(val slots: List<FurnaceSlot>) {

    fun toCounters(): Map<String, Long> = buildMap {
        slots.forEachIndexed { index, slot ->
            slot.input?.let { put("$INPUT$index/$it", slot.inputCount.toLong()) }
            slot.output?.let { put("$OUTPUT$index/$it", slot.outputCount.toLong()) }
            if (slot.progress > 0) put("$PROGRESS$index", slot.progress.toLong())
        }
    }

    fun with(index: Int, slot: FurnaceSlot): FurnaceState =
        FurnaceState(slots.mapIndexed { i, current -> if (i == index) slot else current })

    companion object {
        const val PREFIX = "forno_"
        private const val INPUT = "forno_in/"
        private const val OUTPUT = "forno_out/"
        private const val PROGRESS = "forno_progress/"

        val EMPTY = FurnaceState(List(FurnaceRules.SLOTS) { FurnaceSlot.EMPTY })

        fun fromCounters(counters: Map<String, Long>): FurnaceState = FurnaceState(
            List(FurnaceRules.SLOTS) { index ->
                val input = entry(counters, "$INPUT$index/")
                val output = entry(counters, "$OUTPUT$index/")
                FurnaceSlot(
                    input = input?.first,
                    inputCount = input?.second ?: 0,
                    output = output?.first,
                    outputCount = output?.second ?: 0,
                    progress = counters["$PROGRESS$index"]?.toInt() ?: 0,
                )
            }
        )

        private fun entry(counters: Map<String, Long>, prefix: String): Pair<String, Int>? =
            counters.entries.firstOrNull { it.key.startsWith(prefix) && it.value > 0 }
                ?.let { it.key.removePrefix(prefix) to it.value.toInt() }
    }
}
