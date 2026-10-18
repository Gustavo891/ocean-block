package one_block.system.minions.placed_minion

import one_block.libs.item_builder.ItemSerializer
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object MinionFilter {

    fun entryOf(item: ItemStack): String = ItemSerializer.serialize(item.asOne())

    fun itemOf(entry: String): ItemStack? {
        prototypeOf(entry)?.let { return it }
        return materialOf(entry)?.let { ItemStack(it) }
    }

    fun matches(entry: String, item: ItemStack): Boolean {
        prototypeOf(entry)?.let { return it.isSimilar(item) }
        return materialOf(entry) == item.type
    }

    private fun prototypeOf(entry: String): ItemStack? =
        runCatching { ItemSerializer.deserialize(entry) }.getOrNull()

    private fun materialOf(entry: String): Material? =
        runCatching { Material.valueOf(entry) }.getOrNull()
}
