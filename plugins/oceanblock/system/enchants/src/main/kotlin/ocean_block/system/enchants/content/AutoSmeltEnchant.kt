package ocean_block.system.enchants.content

import ocean_block.system.enchants.enchant.CustomEnchant
import ocean_block.system.enchants.enchant.EnchantRarity
import ocean_block.system.enchants.enchant.EnchantTarget
import ocean_block.system.enchants.enchant.context.BreakContext
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class AutoSmeltEnchant : CustomEnchant() {

    override val id = "auto_smelt"
    override val prettyName = "Fundição"
    override val maxLevel = 1
    override val rarity = EnchantRarity.Epic
    override val targets = setOf(EnchantTarget.Pickaxe)

    override val description = listOf(
        "Minérios são fundidos",
        "automaticamente ao quebrar.",
    )

    override fun onBlockBreak(context: BreakContext) {
        val block = context.block
        val drops = block.getDrops(context.item, context.player)
        if (drops.isEmpty()) return

        val smelted = drops.mapNotNull { drop ->
            val result = SMELT_MAP[drop.type] ?: return@mapNotNull null
            ItemStack(result, drop.amount)
        }

        if (smelted.isEmpty()) return

        context.event.isDropItems = false

        val leftover = context.player.inventory.addItem(*smelted.toTypedArray())
        leftover.values.forEach { block.world.dropItemNaturally(block.location, it) }
    }

    companion object {

        private val SMELT_MAP = mapOf(
            Material.RAW_IRON to Material.IRON_INGOT,
            Material.RAW_GOLD to Material.GOLD_INGOT,
            Material.RAW_COPPER to Material.COPPER_INGOT,
            Material.IRON_ORE to Material.IRON_INGOT,
            Material.DEEPSLATE_IRON_ORE to Material.IRON_INGOT,
            Material.GOLD_ORE to Material.GOLD_INGOT,
            Material.DEEPSLATE_GOLD_ORE to Material.GOLD_INGOT,
            Material.COPPER_ORE to Material.COPPER_INGOT,
            Material.DEEPSLATE_COPPER_ORE to Material.COPPER_INGOT,
            Material.ANCIENT_DEBRIS to Material.NETHERITE_SCRAP,
            Material.COBBLESTONE to Material.STONE,
            Material.SAND to Material.GLASS,
        )
    }
}
