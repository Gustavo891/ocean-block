package one_block.system.enchants.enchant

import one_block.system.enchants.enchant.context.AttackContext
import one_block.system.enchants.enchant.context.BreakContext
import one_block.system.enchants.enchant.context.DamagedContext
import org.bukkit.inventory.ItemStack

abstract class CustomEnchant {

    abstract val id: String
    abstract val prettyName: String
    abstract val maxLevel: Int
    abstract val rarity: EnchantRarity
    abstract val targets: Set<EnchantTarget>

    open val description: List<String> = emptyList()

    open fun onBlockBreak(context: BreakContext) {}

    open fun onAttack(context: AttackContext) {}

    open fun onDamaged(context: DamagedContext) {}

    fun canApplyTo(item: ItemStack): Boolean = targets.any { it.matches(item.type) }

    fun displayName(level: Int): String {
        if (maxLevel == 1) return "${rarity.color}$prettyName"
        return "${rarity.color}$prettyName ${roman(level)}"
    }

    companion object {

        private val ROMAN = listOf(
            1000 to "M", 900 to "CM", 500 to "D", 400 to "CD",
            100 to "C", 90 to "XC", 50 to "L", 40 to "XL",
            10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I",
        )

        fun roman(value: Int): String {
            if (value <= 0) return ""

            var remaining = value
            return buildString {
                ROMAN.forEach { (number, symbol) ->
                    while (remaining >= number) {
                        append(symbol)
                        remaining -= number
                    }
                }
            }
        }
    }
}
