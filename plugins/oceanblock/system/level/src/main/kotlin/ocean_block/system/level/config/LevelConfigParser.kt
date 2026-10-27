package ocean_block.system.level.config

import org.bukkit.configuration.ConfigurationSection

object LevelConfigParser {

    fun parse(config: ConfigurationSection, warn: (String) -> Unit): LevelConfig {
        val defaults = LevelConfig.DEFAULT
        return LevelConfig(
            debrisDefault = number(config, "detritos.padrao", defaults.debrisDefault, warn),
            debris = table(config, "detritos.tipos", defaults.debris, warn),
            generator = table(config, "gerador", defaults.generator, warn),
            recipeBase = number(config, "receitas.base", defaults.recipeBase, warn),
            recipePerLevel = number(config, "receitas.por-nivel", defaults.recipePerLevel, warn),
            mobDefault = number(config, "mobs.padrao", defaults.mobDefault, warn),
            mobs = table(config, "mobs.tipos", defaults.mobs, warn).mapKeys { it.key.uppercase() },
        )
    }

    private fun number(config: ConfigurationSection, path: String, fallback: Int, warn: (String) -> Unit): Int {
        if (!config.contains(path)) return fallback
        val value = positive(config.get(path))
        if (value == null) warn("[nivel] '$path' precisa ser um número >= 0; usando $fallback")
        return value ?: fallback
    }

    private fun table(config: ConfigurationSection, path: String, fallback: Map<String, Int>, warn: (String) -> Unit): Map<String, Int> {
        val section = config.getConfigurationSection(path) ?: return fallback
        return section.getKeys(false).mapNotNull { key ->
            val value = positive(section.get(key))
            if (value == null) warn("[nivel] '$path.$key' precisa ser um número >= 0; ignorado")
            value?.let { key to it }
        }.toMap()
    }

    private fun positive(raw: Any?): Int? = (raw as? Number)?.toInt()?.takeIf { it >= 0 }
}
