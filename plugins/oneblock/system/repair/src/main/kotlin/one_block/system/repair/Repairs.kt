package one_block.system.repair

import one_block.libs.item_builder.*
import one_block.system.repair.enums.Fragment
import one_block.system.repair.enums.Repair
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.*

object Repairs {

    private val UUID_KEY = NamespacedKey("repair", "uuid")
    private val FRAGMENT_KEY = NamespacedKey("repair", "fragment")
    private val REPAIR_KEY = NamespacedKey("repair", "repair")

    fun createRepairItem(repair: Repair) = itemStack(Material.FIREWORK_STAR) {
        meta {
            name("<yellow>Reparo <b>${repair.name}</b>")

            addLore {
                +listOf(
                    "<gray>Restaura <white>${repair.getPercent()}%</white><gray> da",
                    "<gray>durabilidade de um item.",
                    "",
                    "<yellow>Jogue sobre o item",
                    "<yellow>para repará-lo.",
                )
            }

            nbt {
                string(REPAIR_KEY, repair.name)
                string(UUID_KEY, UUID.randomUUID().toString())
            }

        }
    }

    fun createFragmentItem(fragment: Fragment) = itemStack(Material.STONE_BUTTON) {
        meta {
            name("<yellow>Fragmento de reparo <b>${fragment.name}</b>")
            nbt {
                string(FRAGMENT_KEY, fragment.name)
            }
        }
    }

    fun getFragmentByItem(item: ItemStack) =
        item.itemMeta?.persistentDataContainer?.get(FRAGMENT_KEY, PersistentDataType.STRING)

    fun getRepairByItem(item: ItemStack) =
        item.itemMeta?.persistentDataContainer?.get(REPAIR_KEY, PersistentDataType.STRING)
}
