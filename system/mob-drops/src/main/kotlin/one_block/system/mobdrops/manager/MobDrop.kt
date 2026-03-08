package one_block.system.mobdrops.manager

import org.bukkit.inventory.ItemStack

data class MobDrop(
    val item: () -> ItemStack,
    val chance: Double
)