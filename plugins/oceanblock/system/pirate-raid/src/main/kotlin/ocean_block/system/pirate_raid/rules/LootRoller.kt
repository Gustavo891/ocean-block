package ocean_block.system.pirate_raid.rules

import kotlin.random.Random

data class LootEntry(val item: String, val chance: Double, val min: Int, val max: Int)

data class Drop(val item: String, val amount: Int)

object LootRoller {

    const val COIN = "moeda"
    const val PEARL = "perola"

    fun roll(entries: List<LootEntry>, rolls: Int, random: Random): List<Drop> =
        entries.flatMap { entry ->
            val times = if (entry.item == COIN) 1 else rolls.coerceAtLeast(1)
            (1..times).mapNotNull { rollOne(entry, random) }
        }

    private fun rollOne(entry: LootEntry, random: Random): Drop? {
        if (entry.chance <= 0.0 || random.nextDouble() >= entry.chance) return null
        return Drop(entry.item, random.nextInt(entry.min, entry.max + 1))
    }
}
