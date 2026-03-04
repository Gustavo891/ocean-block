package one_block.system.minions.forge

import org.bukkit.inventory.ItemStack

data class ForgeRequirement(
    val prettyName: String,
    val item: ItemStack,
    val amount: Int
)