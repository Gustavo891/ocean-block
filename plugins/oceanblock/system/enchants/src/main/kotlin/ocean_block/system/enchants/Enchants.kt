package ocean_block.system.enchants

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import ocean_block.libs.formatter.Mini
import ocean_block.system.enchants.enchant.CustomEnchant
import ocean_block.system.enchants.enchant.EnchantKeys
import ocean_block.system.enchants.enchant.EnchantRegistry
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

object Enchants {

    private const val ENTRY_DELIMITER = ";"
    private const val VALUE_DELIMITER = ":"

    fun all(item: ItemStack?): Map<CustomEnchant, Int> {
        val meta = item?.itemMeta ?: return emptyMap()
        return read(meta).mapNotNull { (id, level) ->
            val enchant = EnchantRegistry.find(id) ?: return@mapNotNull null
            enchant to level
        }.toMap()
    }

    fun levelOf(item: ItemStack?, enchant: CustomEnchant): Int {
        val meta = item?.itemMeta ?: return 0
        return read(meta)[enchant.id] ?: 0
    }

    fun has(item: ItemStack?, enchant: CustomEnchant): Boolean = levelOf(item, enchant) > 0

    fun apply(item: ItemStack, enchant: CustomEnchant, level: Int): Boolean {
        if (!enchant.canApplyTo(item)) return false

        val meta = item.itemMeta ?: return false
        val current = read(meta)
        val capped = level.coerceIn(1, enchant.maxLevel)

        if ((current[enchant.id] ?: 0) >= capped) return false

        current[enchant.id] = capped
        write(meta, current)
        item.itemMeta = meta

        return true
    }

    fun remove(item: ItemStack, enchant: CustomEnchant) {
        val meta = item.itemMeta ?: return
        val current = read(meta)
        if (current.remove(enchant.id) == null) return

        write(meta, current)
        item.itemMeta = meta
    }

    private fun read(meta: ItemMeta): MutableMap<String, Int> {
        val raw = meta.persistentDataContainer.get(EnchantKeys.ENCHANTS, PersistentDataType.STRING)
            ?: return mutableMapOf()

        val result = mutableMapOf<String, Int>()

        raw.split(ENTRY_DELIMITER).forEach { entry ->
            if (entry.isBlank()) return@forEach

            val parts = entry.split(VALUE_DELIMITER)
            if (parts.size != 2) return@forEach

            val level = parts[1].toIntOrNull() ?: return@forEach
            result[parts[0]] = level
        }

        return result
    }

    private fun write(meta: ItemMeta, values: Map<String, Int>) {
        val container = meta.persistentDataContainer

        if (values.isEmpty()) {
            container.remove(EnchantKeys.ENCHANTS)
        } else {
            val encoded = values.entries.joinToString(ENTRY_DELIMITER) { "${it.key}$VALUE_DELIMITER${it.value}" }
            container.set(EnchantKeys.ENCHANTS, PersistentDataType.STRING, encoded)
        }

        updateLore(meta, values)
    }

    private fun updateLore(meta: ItemMeta, values: Map<String, Int>) {
        val container = meta.persistentDataContainer
        val previous = container.get(EnchantKeys.LORE_LINES, PersistentDataType.INTEGER) ?: 0

        val lore = meta.lore()?.toMutableList() ?: mutableListOf()
        repeat(previous.coerceAtMost(lore.size)) { lore.removeAt(0) }

        val lines = mutableListOf<Component>()

        values.forEach { (id, level) ->
            val enchant = EnchantRegistry.find(id) ?: return@forEach
            lines += Mini.deserialize(enchant.displayName(level)).decoration(TextDecoration.ITALIC, false)
        }

        if (lines.isNotEmpty()) lines += Component.empty()

        lore.addAll(0, lines)
        meta.lore(lore)

        if (lines.isEmpty()) {
            container.remove(EnchantKeys.LORE_LINES)
        } else {
            container.set(EnchantKeys.LORE_LINES, PersistentDataType.INTEGER, lines.size)
        }
    }
}
