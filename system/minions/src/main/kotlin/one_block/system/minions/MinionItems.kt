package one_block.system.minions

import one_block.libs.instance.Instance
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object MinionItems {

    private val MACHINE_PART_KEY by lazy {
        NamespacedKey(Instance, "machine_part")
    }

    fun machinePart(): ItemStack {
        val item = ItemStack(Material.IRON_NUGGET)
        val meta = item.itemMeta!!

        meta.setDisplayName("§ePeça Mecânica")

        meta.lore = listOf(
            "§7Fragmento usado para",
            "§7construir Minions."
        )

        // Aqui está o mais importante
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