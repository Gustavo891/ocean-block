package ocean_block.system.level.menu

import ocean_block.system.level.config.LevelConfig
import ocean_block.system.level.curve.LevelCurve
import java.text.NumberFormat
import java.util.Locale

object LevelLore {

    private val LOCALE: Locale = Locale.forLanguageTag("pt-BR")

    fun header(xp: Long): Map<String, String> {
        val level = LevelCurve.levelOf(xp)
        if (level >= LevelCurve.MAX_LEVEL) {
            return mapOf(
                "nivel" to "Nível $level",
                "xp" to "Nível máximo",
                "prox" to "Você chegou ao topo",
                "pct" to "100%",
                "barra" to "100",
            )
        }
        val inLevel = xp - LevelCurve.totalFor(level)
        val needed = LevelCurve.toNext(level)
        val fraction = inLevel.toDouble() / needed
        return mapOf(
            "nivel" to "Nível $level",
            "xp" to "${number(inLevel)} / ${number(needed)} XP",
            "prox" to "Faltam ${number(needed - inLevel)} XP para o nível ${level + 1}",
            "pct" to String.format(LOCALE, "%.2f%%", fraction * 100),
            "barra" to (fraction * 100).toInt().coerceIn(0, 100).toString(),
        )
    }


    fun sources(config: LevelConfig, recipeLevels: Collection<Int>): List<Map<String, String>> {
        val recipeXp = recipeLevels.ifEmpty { listOf(1) }.map(config::recipeXp)
        return listOf(
            source("fishing_rod", "Detritos", config.debris.values + config.debrisDefault, "Puxe detritos com o Gancho ou a Rede."),
            source("cobblestone", "Gerador", config.generator.values, "Quebre blocos do gerador de pedra. Minérios raros valem mais."),
            source("crafting_table", "Receitas", recipeXp, "Fabrique em /receitas. Receitas de nível alto valem mais."),
        )
    }

    private fun source(icon: String, label: String, values: Collection<Int>, hint: String): Map<String, String> =
        mapOf("icone" to icon, "texto" to "$label: ${range(values)}", "dica" to hint)

    private fun range(values: Collection<Int>): String {
        val min = values.minOrNull() ?: 0
        val max = values.maxOrNull() ?: 0
        return if (min == max) "${number(min.toLong())} XP" else "${number(min.toLong())} a ${number(max.toLong())} XP"
    }

    private fun number(value: Long): String = NumberFormat.getIntegerInstance(LOCALE).format(value)
}
