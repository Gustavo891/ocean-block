package ocean_block.system.custom_items

import ocean_block.system.custom_items.block.CustomBlockKeys
import ocean_block.system.custom_items.item.CustomItemKeys
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object CustomIds {

    fun of(stack: ItemStack?): String? {
        val container = stack?.takeIf { it.hasItemMeta() }?.itemMeta?.persistentDataContainer ?: return null
        return container.get(CustomItemKeys.ID, PersistentDataType.STRING)
            ?: container.get(CustomBlockKeys.ID, PersistentDataType.STRING)
    }
}
