package one_block.system.minions.forge

import org.bukkit.inventory.ItemStack

data class ForgeRecipe(
    val id: String,
    val icon: ItemStack,
    val result: ItemStack,
    val requirements: List<ForgeRequirement>
)
