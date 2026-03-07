package one_block.system.craft.forge

import org.bukkit.inventory.ItemStack

data class ForgeRequirement(
    val prettyName: String,
    val item: ItemStack,
    val amount: Int
)