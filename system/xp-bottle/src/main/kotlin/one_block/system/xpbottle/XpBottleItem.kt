package one_block.system.xpbottle

import one_block.libs.instance.Instance
import one_block.libs.item_builder.*
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack

object XpBottleItem {

    private const val KEY = "xp_levels"

    fun create(levels: Int) = itemStack(Material.EXPERIENCE_BOTTLE) {
        meta {
            name("<green>XP Bottle")

            addLore {
                +"<gray>Contém $levels níveis"
                +"<gray>Clique direito para resgatar"
            }

            nbt {
                int(NamespacedKey(Instance, KEY), Experience.getExpFromLevel(levels))
            }
        }
    }

    fun getLevels(item: ItemStack): Int {
        return item.getNbtInt(NamespacedKey(Instance, KEY), 0)
    }
}