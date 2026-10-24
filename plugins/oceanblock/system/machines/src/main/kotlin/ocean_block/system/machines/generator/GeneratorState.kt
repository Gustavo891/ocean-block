package ocean_block.system.machines.generator

import ocean_block.system.machines.energy.EnergyRules

data class GeneratorState(
    val slots: List<FuelStack?>,
    val burnTicks: Int,
    val energy: Long,
) {

    val hasFuel: Boolean get() = slots.any { it != null }

    fun toCounters(): Map<String, Long> = buildMap {
        put(EnergyRules.KEY, energy)
        put(BURN, burnTicks.toLong())
        slots.forEachIndexed { index, stack -> stack?.let { put("$SLOT$index/${it.material}", it.amount.toLong()) } }
    }

    companion object {
        const val PREFIX = "gerador_"
        const val SLOTS = 14
        private const val BURN = "gerador_burn"
        private const val SLOT = "gerador_slot/"
        private const val LEGACY_FUEL = "gerador_fuel/"

        val EMPTY = GeneratorState(List(SLOTS) { null }, 0, 0)

        fun fromCounters(counters: Map<String, Long>): GeneratorState {
            val slots = MutableList<FuelStack?>(SLOTS) { null }
            counters.forEach { (key, value) ->
                if (!key.startsWith(SLOT) || value <= 0) return@forEach
                val (index, material) = key.removePrefix(SLOT).split('/', limit = 2).takeIf { it.size == 2 } ?: return@forEach
                index.toIntOrNull()?.takeIf { it in 0 until SLOTS }?.let { slots[it] = FuelStack(material, value.toInt()) }
            }
            counters.entries.firstOrNull { it.key.startsWith(LEGACY_FUEL) && it.value > 0 }?.let { legacy ->
                val free = slots.indexOf(null)
                if (free >= 0) slots[free] = FuelStack(legacy.key.removePrefix(LEGACY_FUEL), legacy.value.toInt())
            }
            return GeneratorState(
                slots = slots.toList(),
                burnTicks = counters[BURN]?.toInt() ?: 0,
                energy = counters[EnergyRules.KEY] ?: 0,
            )
        }
    }
}
