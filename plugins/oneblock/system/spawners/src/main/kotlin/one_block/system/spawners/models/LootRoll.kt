package one_block.system.spawners.models

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

object LootRoll {

    fun rollMany(table: List<LootEntry>, times: Int, random: Random = Random.Default): List<ItemStack> {
        if (times <= 0) return emptyList()
        if (times == 1) return roll(table, random)

        val totals = mutableMapOf<Material, Int>()
        repeat(times) {
            roll(table, random).forEach { stack -> totals.merge(stack.type, stack.amount, Int::plus) }
        }

        return totals.flatMap { (material, total) -> split(material, total) }
    }

    private fun split(material: Material, total: Int): List<ItemStack> {
        val max = material.maxStackSize.coerceAtLeast(1)
        val stacks = mutableListOf<ItemStack>()

        var remaining = total
        while (remaining > 0) {
            val amount = minOf(remaining, max)
            stacks += ItemStack(material, amount)
            remaining -= amount
        }

        return stacks
    }

    fun roll(table: List<LootEntry>, random: Random = Random.Default): List<ItemStack> =
        table.mapNotNull { entry ->
            if (random.nextDouble() >= entry.chance) return@mapNotNull null

            val amount = if (entry.max <= entry.min) {
                entry.min
            } else {
                random.nextInt(entry.min, entry.max + 1)
            }
            ItemStack(entry.material, amount)
        }
}
