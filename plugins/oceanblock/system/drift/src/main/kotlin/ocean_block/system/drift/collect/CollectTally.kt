package ocean_block.system.drift.collect

class CollectTally(private val windowTicks: Long) {

    private class Entry(val total: Int, val expiresAt: Long)

    private val entries = mutableMapOf<String, Entry>()

    val size: Int get() = entries.size

    fun add(key: String, amount: Int, nowTick: Long): Int {
        entries.values.removeIf { it.expiresAt <= nowTick }
        val total = (entries[key]?.total ?: 0) + amount
        entries[key] = Entry(total, nowTick + windowTicks)
        return total
    }
}
