package one_block.system.fly_timer

import one_block.libs.item_builder.addLore
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.item_builder.nbt
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

object Flys {

    private val UUID_KEY = NamespacedKey("flys", "uuid")
    private val TIMER_KEY = NamespacedKey("flys", "timer")

    fun createFlyItem(timer: Long = 60) = itemStack(Material.FEATHER) {
        meta {
            name("<yellow>Pena de Voo")

            addLore {
                +"<gray>Ative esta pena mágica"
                +"<gray>e receba o <yellow>poder de voar"
                +"<gray>por <yellow>1m<gray>."
                +""
                +"<green>Clique para ativar"
            }

            nbt {
                long(TIMER_KEY, timer)
                string(UUID_KEY, UUID.randomUUID().toString())
            }
        }
    }

    fun getTimer(item: ItemStack): Long {
        return item.itemMeta?.persistentDataContainer?.get(TIMER_KEY, PersistentDataType.LONG) ?: 0
    }
}