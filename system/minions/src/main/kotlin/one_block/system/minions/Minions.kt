package one_block.system.minions

import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.item_builder.nbt
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

object Minions {

    private val FUEL_KEY = NamespacedKey("minions", "fuel")

    fun createFuelItem(amount: Int) = itemStack(Material.COAL) {
        meta<ItemMeta> {
            name("<yellow>Fuel: $amount")

            nbt {
                int(FUEL_KEY, amount)
            }
        }
    }

    fun getFuelAmount(item: ItemStack): Int {
        return item.itemMeta?.persistentDataContainer?.get(FUEL_KEY, PersistentDataType.INTEGER) ?: 0
    }
}