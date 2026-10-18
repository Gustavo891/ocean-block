package one_block.system.enchants.book

import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.item_builder.nbt
import one_block.libs.item_builder.setLore
import one_block.system.enchants.enchant.CustomEnchant
import one_block.system.enchants.enchant.EnchantKeys
import one_block.system.enchants.enchant.EnchantRegistry
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

object EnchantBooks {

    fun create(enchant: CustomEnchant, level: Int): ItemStack {
        val capped = level.coerceIn(1, enchant.maxLevel)

        return itemStack(Material.ENCHANTED_BOOK) {
            meta<ItemMeta> {
                name(enchant.displayName(capped))

                setLore {
                    +"<dark_gray>▸ <white>Raridade: ${enchant.rarity.formatted()}"
                    +"<dark_gray>▸ <white>Chance: <yellow>${(enchant.rarity.successChance * 100).toInt()}%"
                    +""
                    enchant.description.forEach { +"<gray>$it" }
                    if (enchant.description.isNotEmpty()) +""
                    +"<dark_gray>Aplicável em: <gray>${enchant.targets.joinToString { it.prettyName }}"
                    +"<dark_gray>Arraste sobre o item para aplicar."
                }

                nbt {
                    string(EnchantKeys.BOOK_ENCHANT, enchant.id)
                    int(EnchantKeys.BOOK_LEVEL, capped)
                }
            }
        }
    }

    fun enchantOf(item: ItemStack?): CustomEnchant? {
        val container = item?.itemMeta?.persistentDataContainer ?: return null
        val id = container.get(EnchantKeys.BOOK_ENCHANT, PersistentDataType.STRING) ?: return null
        return EnchantRegistry.find(id)
    }

    fun levelOf(item: ItemStack?): Int {
        val container = item?.itemMeta?.persistentDataContainer ?: return 0
        return container.get(EnchantKeys.BOOK_LEVEL, PersistentDataType.INTEGER) ?: 0
    }

    fun isBook(item: ItemStack?): Boolean = enchantOf(item) != null
}
