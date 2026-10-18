package ocean_block.system.collections.collection

import java.util.UUID

data class CollectionData(
    val uuid: UUID,
    val name: String,
    val amounts: MutableMap<String, Long> = mutableMapOf(),
) {

    fun amountOf(entry: CollectionEntry): Long = amounts.getOrDefault(entry.id, 0L)

    fun tierOf(entry: CollectionEntry): Int = entry.tierFor(amountOf(entry))

    fun add(entry: CollectionEntry, amount: Long): Int {
        if (amount <= 0) return tierOf(entry)

        val before = tierOf(entry)
        amounts[entry.id] = amountOf(entry) + amount

        return tierOf(entry) - before
    }

    fun set(entry: CollectionEntry, amount: Long) {
        amounts[entry.id] = amount.coerceAtLeast(0)
    }

    fun nextTierAmount(entry: CollectionEntry): Long? {
        val tier = tierOf(entry)
        if (tier >= entry.maxTier) return null
        return entry.amountForTier(tier + 1)
    }
}
