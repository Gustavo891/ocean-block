package ocean_block.system.enchants.content

import ocean_block.system.enchants.enchant.CustomEnchant
import ocean_block.system.enchants.enchant.EnchantRarity
import ocean_block.system.enchants.enchant.EnchantTarget
import ocean_block.system.enchants.enchant.context.BreakContext

class TelekinesisEnchant : CustomEnchant() {

    override val id = "telekinesis"
    override val prettyName = "Telecinese"
    override val maxLevel = 1
    override val rarity = EnchantRarity.Rare
    override val targets = EnchantTarget.TOOLS

    override val description = listOf(
        "Os drops vão direto para o",
        "inventário em vez de cair no chão.",
    )

    override fun onBlockBreak(context: BreakContext) {
        val player = context.player
        val block = context.block
        val drops = block.getDrops(context.item, player)

        if (drops.isEmpty()) return

        context.event.isDropItems = false

        val leftover = player.inventory.addItem(*drops.toTypedArray())
        leftover.values.forEach { block.world.dropItemNaturally(block.location, it) }
    }
}
