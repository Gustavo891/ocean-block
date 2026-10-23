package ocean_block.system.islands.generator

import org.bukkit.configuration.ConfigurationSection
import kotlin.random.Random

data class GeneratorWeight(val block: String, val weight: Int)

class GeneratorTable(private val levels: Map<Int, List<GeneratorWeight>>) {

    val maxLevel: Int = levels.keys.maxOrNull() ?: 0

    fun weights(level: Int): List<GeneratorWeight> =
        levels.filterKeys { it <= level }.maxByOrNull { it.key }?.value ?: FALLBACK

    companion object {
        val FALLBACK = listOf(GeneratorWeight("COBBLESTONE", 1))
    }
}

object GeneratorTableParser {

    private const val LEVELS = "niveis"

    fun parse(config: ConfigurationSection, isBlock: (String) -> Boolean, warn: (String) -> Unit): GeneratorTable {
        val section = config.getConfigurationSection(LEVELS)
        val levels = section?.getKeys(false).orEmpty().mapNotNull { key ->
            val level = key.toIntOrNull()?.takeIf { it >= 0 } ?: run {
                warn("[gerador] nível '$key' não é um número; ignorado")
                return@mapNotNull null
            }
            val weights = section?.getConfigurationSection(key)?.let { weights(it, key, isBlock, warn) }.orEmpty()
            if (weights.isEmpty()) null else level to weights
        }.toMap()
        if (levels.isEmpty()) warn("[gerador] nenhum nível válido em '$LEVELS'; o gerador só forma pedregulho")
        return GeneratorTable(levels)
    }

    private fun weights(section: ConfigurationSection, level: String, isBlock: (String) -> Boolean, warn: (String) -> Unit): List<GeneratorWeight> =
        section.getKeys(false).mapNotNull { block ->
            val weight = section.getInt(block, -1)
            when {
                !isBlock(block) -> {
                    warn("[gerador] nível $level: '$block' não é um bloco; ignorado")
                    null
                }
                weight <= 0 -> {
                    warn("[gerador] nível $level: peso de '$block' precisa ser maior que zero; ignorado")
                    null
                }
                else -> GeneratorWeight(block, weight)
            }
        }
}

object GeneratorRoller {

    fun roll(table: GeneratorTable, level: Int, random: Random): String {
        val weights = table.weights(level)
        var pick = random.nextInt(weights.sumOf { it.weight })
        for (entry in weights) {
            if (pick < entry.weight) return entry.block
            pick -= entry.weight
        }
        return weights.last().block
    }
}
