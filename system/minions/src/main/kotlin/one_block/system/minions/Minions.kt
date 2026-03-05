package one_block.system.minions

import one_block.libs.instance.Instance
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.item_builder.nbt
import one_block.system.minions.enums.MinionType
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

object Minions {

    private val FUEL_KEY = NamespacedKey("minions", "fuel")

    private val MACHINE_PART_KEY by lazy {
        NamespacedKey(Instance, "machine_part")
    }

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

    fun createMinionItem(type: MinionType) = itemStack(Material.STONE) {
        meta {
            name(type.name)
        }
    }

    fun machinePart(): ItemStack {
        val item = ItemStack(Material.OAK_BUTTON)
        val meta = item.itemMeta!!

        meta.setDisplayName("§ePeça Mecânica")

        meta.lore = listOf(
            "§7Fragmento usado para",
            "§7construir Minions."
        )

        meta.persistentDataContainer.set(
            MACHINE_PART_KEY,
            PersistentDataType.BOOLEAN,
            true
        )

        item.itemMeta = meta
        return item
    }

    fun isMachinePart(item: ItemStack?): Boolean {
        if (item == null) return false
        val meta = item.itemMeta ?: return false

        return meta.persistentDataContainer.has(
           MACHINE_PART_KEY,
            PersistentDataType.BOOLEAN
        )
    }
}