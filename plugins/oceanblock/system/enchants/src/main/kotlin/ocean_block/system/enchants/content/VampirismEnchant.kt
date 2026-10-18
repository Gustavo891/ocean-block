package ocean_block.system.enchants.content

import ocean_block.system.enchants.enchant.CustomEnchant
import ocean_block.system.enchants.enchant.EnchantRarity
import ocean_block.system.enchants.enchant.EnchantTarget
import ocean_block.system.enchants.enchant.context.AttackContext
import org.bukkit.attribute.Attribute

class VampirismEnchant : CustomEnchant() {

    override val id = "vampirism"
    override val prettyName = "Vampirismo"
    override val maxLevel = 5
    override val rarity = EnchantRarity.Legendary
    override val targets = setOf(EnchantTarget.Sword)

    override val description = listOf(
        "Recupera vida proporcional",
        "ao dano causado.",
    )

    override fun onAttack(context: AttackContext) {
        val attacker = context.attacker
        val healed = context.event.finalDamage * HEAL_PER_LEVEL * context.level
        if (healed <= 0) return

        val max = attacker.getAttribute(Attribute.MAX_HEALTH)?.value ?: return
        attacker.health = (attacker.health + healed).coerceAtMost(max)
    }

    companion object {
        private const val HEAL_PER_LEVEL = 0.05
    }
}
