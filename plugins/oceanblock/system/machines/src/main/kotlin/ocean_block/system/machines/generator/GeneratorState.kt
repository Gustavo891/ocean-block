package ocean_block.system.machines.generator

import ocean_block.system.machines.energy.EnergyRules

data class GeneratorState(
    val fuel: String?,
    val fuelCount: Int,
    val burnTicks: Int,
    val energy: Long,
) {

    fun toCounters(): Map<String, Long> = buildMap {
        put(EnergyRules.KEY, energy)
        put(BURN, burnTicks.toLong())
        fuel?.let { put(FUEL + it, fuelCount.toLong()) }
    }

    companion object {
        const val PREFIX = "gerador_"
        private const val BURN = "gerador_burn"
        private const val FUEL = "gerador_fuel/"

        val EMPTY = GeneratorState(null, 0, 0, 0)

        fun fromCounters(counters: Map<String, Long>): GeneratorState {
            val fuel = counters.entries.firstOrNull { it.key.startsWith(FUEL) && it.value > 0 }
            return GeneratorState(
                fuel = fuel?.key?.removePrefix(FUEL),
                fuelCount = fuel?.value?.toInt() ?: 0,
                burnTicks = counters[BURN]?.toInt() ?: 0,
                energy = counters[EnergyRules.KEY] ?: 0,
            )
        }
    }
}
