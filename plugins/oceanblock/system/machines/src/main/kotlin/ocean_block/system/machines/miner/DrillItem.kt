package ocean_block.system.machines.miner

import ocean_block.system.custom_items.item.CustomItem
import org.bukkit.Material
import org.bukkit.NamespacedKey

class DrillItem private constructor(val tier: DrillTier, override val id: String) : CustomItem() {

    override val material: Material = Material.PAPER
    override val itemModel: NamespacedKey = MinerDisplay.drillModel(tier)
    override val displayName: String = name(tier)
    override val lore: List<String> = details(tier) + listOf("", "<yellow>Encaixe no menu do Minerador.")

    companion object {

        val all: List<DrillItem> = DrillTier.entries.mapNotNull { tier -> tier.itemId?.let { DrillItem(tier, it) } }

        fun of(tier: DrillTier): DrillItem? = all.firstOrNull { it.tier == tier }

        fun name(tier: DrillTier): String = "${tier.color}Broca de ${tier.label}"

        fun details(tier: DrillTier): List<String> = listOf(
            "<gray>Quebra um bloco a cada <white>${tier.secondsLabel} s<gray>.",
            "<gray>Puxa <yellow>${tier.energyPerSecond} E/s <gray>com o minerador ligado.",
        )
    }
}
