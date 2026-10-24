package ocean_block.system.machines.generator

data class FuelStack(val material: String, val amount: Int)

object GeneratorView {

    private const val BAR_CHAR = "|"
    private const val TICKS_PER_SECOND = 20

    fun bar(energy: Long, capacity: Long, width: Int): Pair<String, String> {
        val raw = (energy * width / capacity).toInt().coerceIn(0, width)
        val filled = if (energy > 0 && raw == 0) 1 else raw
        return BAR_CHAR.repeat(filled) to BAR_CHAR.repeat(width - filled)
    }

    fun fuelEntries(items: List<Pair<String, Int>>, fuelTicks: (String) -> Int?): List<FuelStack> =
        items.filter { (material, _) -> fuelTicks(material) != null }
            .groupBy({ it.first }, { it.second })
            .map { (material, amounts) -> FuelStack(material, amounts.sum()) }

    fun burnSeconds(state: GeneratorState, fuelTicks: (String) -> Int?): Int {
        val stored = state.fuel?.let { fuelTicks(it) }?.let { it * state.fuelCount } ?: 0
        return (state.burnTicks + stored) / TICKS_PER_SECOND
    }
}
