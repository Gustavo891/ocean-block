package one_block.system.craft.forge

import org.bukkit.inventory.ItemStack

data class ForgeRequirement(
    val prettyName: String,
    val item: ItemStack,
    val amount: Int,
    val matcher: ((ItemStack) -> Boolean)? = null
) {

    fun matches(stack: ItemStack): Boolean {
        return matcher?.invoke(stack) ?: stack.isSimilar(item)
    }
}