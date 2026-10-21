package ocean_block.system.stations.sieve

import org.bukkit.configuration.ConfigurationSection

object SieveTableParser {

    private val TABLES = SieveTier.entries.map { it.table }.distinct()

    fun parse(root: ConfigurationSection, isOutput: (String) -> Boolean, warn: (String) -> Unit): SieveTable {
        val drops = SieveInput.entries.associateWith { input ->
            val section = root.getConfigurationSection(input.key)
            TABLES.associateWith { table ->
                section?.getMapList(table).orEmpty().mapNotNull { raw -> drop("${input.key}.$table", raw, isOutput, warn) }
            }
        }
        return SieveTable(drops)
    }

    private fun drop(where: String, raw: Map<*, *>, isOutput: (String) -> Boolean, warn: (String) -> Unit): SieveDrop? {
        val output = raw["saida"]?.toString()?.trim().orEmpty()
        val min = (raw["min"] as? Number)?.toInt() ?: 1
        val max = (raw["max"] as? Number)?.toInt() ?: min
        val chance = (raw["chance"] as? Number)?.toDouble()
        return when {
            !isOutput(output) -> null.also { warn("[peneira] $where: saida '$output' desconhecida; ignorada") }
            chance == null || chance < 0.0 || chance > 1.0 -> null.also { warn("[peneira] $where: chance de '$output' fora de 0..1; ignorada") }
            min < 1 || max < min -> null.also { warn("[peneira] $where: min/max de '$output' invalidos; ignorada") }
            else -> SieveDrop(output, min, max, chance)
        }
    }
}
