package one_block.system.craft.forge

import one_block.system.craft.CraftCategory
import org.bukkit.inventory.ItemStack

data class ForgeRecipe(
    val id: String,
    val prettyName: String,
    val icon: ItemStack,
    val result: ItemStack,
    val requirements: List<ForgeRequirement>,
    val category: CraftCategory = CraftCategory.ESPECIAIS,
)
