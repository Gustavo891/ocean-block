package ocean_block.system.enchants.enchant

import ocean_block.libs.instance.Instance
import org.bukkit.NamespacedKey

object EnchantKeys {

    val ENCHANTS: NamespacedKey by lazy { NamespacedKey(Instance, "enchants") }

    val LORE_LINES: NamespacedKey by lazy { NamespacedKey(Instance, "enchant_lore_lines") }

    val BOOK_ENCHANT: NamespacedKey by lazy { NamespacedKey(Instance, "enchant_book_id") }

    val BOOK_LEVEL: NamespacedKey by lazy { NamespacedKey(Instance, "enchant_book_level") }
}
