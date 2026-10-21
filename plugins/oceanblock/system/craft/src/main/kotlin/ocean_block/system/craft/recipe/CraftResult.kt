package ocean_block.system.craft.recipe

import org.bukkit.inventory.ItemStack

sealed interface CraftResult {

    data class Crafted(val item: ItemStack) : CraftResult

    data class Missing(val shortages: List<Shortage>) : CraftResult

    data class Locked(val research: String) : CraftResult
}
