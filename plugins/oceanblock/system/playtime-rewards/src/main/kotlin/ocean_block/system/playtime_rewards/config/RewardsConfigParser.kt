package ocean_block.system.playtime_rewards.config

import ocean_block.system.playtime_rewards.model.ItemPrize
import ocean_block.system.playtime_rewards.model.Milestone
import org.bukkit.configuration.ConfigurationSection

object RewardsConfigParser {

    private const val TAG = "[recompensas]"
    private const val DEFAULT_ICON = "clock"

    fun parse(config: ConfigurationSection, isItem: (String) -> Boolean, warn: (String) -> Unit): List<Milestone> {
        val section = config.getConfigurationSection("marcos") ?: return emptyList()
        val parsed = section.getKeys(false).mapNotNull { id ->
            section.getConfigurationSection(id)?.let { milestone(id, it, isItem, warn) }
        }
        val seen = mutableSetOf<Int>()
        return parsed.filter { milestone ->
            val fresh = seen.add(milestone.minutes)
            if (!fresh) warn("$TAG marco '${milestone.id}' repete ${milestone.minutes} minutos; ignorado")
            fresh
        }.sortedBy { it.minutes }
    }

    private fun milestone(
        id: String,
        section: ConfigurationSection,
        isItem: (String) -> Boolean,
        warn: (String) -> Unit,
    ): Milestone? {
        val minutes = (section.get("minutos") as? Number)?.toInt()
        if (minutes == null || minutes <= 0) {
            warn("$TAG marco '$id' precisa de 'minutos' maior que 0; ignorado")
            return null
        }
        return Milestone(
            id = id,
            minutes = minutes,
            coins = amount(section, id, "coins", warn),
            pearls = amount(section, id, "perolas", warn),
            xp = amount(section, id, "xp", warn),
            icon = icon(section, id, isItem, warn),
            items = section.getStringList("itens").mapNotNull { item(it, id, isItem, warn) },
            commands = section.getStringList("comandos"),
        )
    }

    private fun amount(section: ConfigurationSection, id: String, key: String, warn: (String) -> Unit): Int {
        if (!section.contains(key)) return 0
        val value = (section.get(key) as? Number)?.toInt()?.takeIf { it >= 0 }
        if (value == null) warn("$TAG '$id.$key' precisa ser um número >= 0; usando 0")
        return value ?: 0
    }

    private fun icon(section: ConfigurationSection, id: String, isItem: (String) -> Boolean, warn: (String) -> Unit): String {
        val raw = section.getString("icone") ?: return DEFAULT_ICON
        if (isItem(raw)) return raw.lowercase()
        warn("$TAG '$id.icone' não é um item: $raw; usando $DEFAULT_ICON")
        return DEFAULT_ICON
    }

    private fun item(raw: String, id: String, isItem: (String) -> Boolean, warn: (String) -> Unit): ItemPrize? {
        val material = raw.substringBefore(':').trim()
        val amount = raw.substringAfter(':', "1").trim().toIntOrNull()?.takeIf { it > 0 }
        if (!isItem(material) || amount == null) {
            warn("$TAG '$id.itens' inválido: $raw (use MATERIAL:quantidade); ignorado")
            return null
        }
        return ItemPrize(material.uppercase(), amount)
    }
}
