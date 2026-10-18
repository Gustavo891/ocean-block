package one_block.system.islands.monster

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment

data class WeaponConfig(
    val material: Material?,
    val enchantments: Map<Enchantment, Int>?,
) {
}
