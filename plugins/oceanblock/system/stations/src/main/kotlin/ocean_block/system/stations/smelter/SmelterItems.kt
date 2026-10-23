package ocean_block.system.stations.smelter

import ocean_block.system.custom_items.CustomIds
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object SmelterItems {

    fun idOf(stack: ItemStack?): String? {
        if (stack == null || stack.type.isAir) return null
        return CustomIds.of(stack) ?: stack.type.name
    }

    fun isVanilla(stack: ItemStack?): Boolean = stack != null && !stack.type.isAir && CustomIds.of(stack) == null

    fun stackOf(id: String, amount: Int): ItemStack? = Material.matchMaterial(id)?.let { ItemStack(it, amount) }
}
