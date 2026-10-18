package one_block.system.skills.perk

import one_block.system.skills.Skills
import one_block.system.skills.skill.SkillPerk
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.inventory.ItemStack
import java.util.concurrent.ThreadLocalRandom

class MiningPerks : Listener {

    private companion object {

        const val EXTRA_FORTUNE_CHANCE = 0.5

        val SMELT_RESULT = mapOf(
            Material.RAW_IRON to Material.IRON_INGOT,
            Material.RAW_GOLD to Material.GOLD_INGOT,
            Material.RAW_COPPER to Material.COPPER_INGOT,
            Material.ANCIENT_DEBRIS to Material.NETHERITE_SCRAP,
        )

        val FORTUNE_AFFECTED = setOf(
            Material.RAW_IRON, Material.RAW_GOLD, Material.RAW_COPPER, Material.ANCIENT_DEBRIS,
            Material.IRON_INGOT, Material.GOLD_INGOT, Material.COPPER_INGOT, Material.NETHERITE_SCRAP,
            Material.COAL, Material.DIAMOND, Material.EMERALD, Material.LAPIS_LAZULI,
            Material.REDSTONE, Material.QUARTZ, Material.AMETHYST_SHARD,
        )
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockDropItem(event: BlockDropItemEvent) {
        val player = event.player

        val fortuneChance = Skills.perkPower(player, SkillPerk.ExtraFortune) * EXTRA_FORTUNE_CHANCE
        val smeltChance = Skills.perkPower(player, SkillPerk.Smelting)

        if (fortuneChance <= 0.0 && smeltChance <= 0.0) return

        for (item in event.items) {
            val stack = item.itemStack
            if (stack.type !in FORTUNE_AFFECTED) continue

            val amount = if (chance(fortuneChance)) stack.amount + 1 else stack.amount
            val type = if (chance(smeltChance)) SMELT_RESULT[stack.type] ?: stack.type else stack.type

            if (amount == stack.amount && type == stack.type) continue

            item.itemStack = ItemStack(type, amount)
        }
    }

    private fun chance(probability: Double): Boolean =
        ThreadLocalRandom.current().nextDouble() < probability
}
