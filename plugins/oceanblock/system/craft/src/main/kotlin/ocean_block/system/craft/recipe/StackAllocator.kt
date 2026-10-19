package ocean_block.system.craft.recipe

class Requirement<T>(val amount: Int, val accepts: (T) -> Boolean)

data class Allocation(
    val remaining: List<Int>,
    val taken: List<Int>,
    val missing: List<Int>,
) {
    val complete: Boolean get() = missing.all { it == 0 }
}

object StackAllocator {

    fun <T> count(items: List<T?>, amountOf: (T) -> Int, accepts: (T) -> Boolean): Int =
        items.sumOf { item -> if (item != null && accepts(item)) amountOf(item).coerceAtLeast(0) else 0 }

    fun <T> allocate(items: List<T?>, amountOf: (T) -> Int, requirements: List<Requirement<T>>): Allocation {
        val remaining = items.map { item -> item?.let(amountOf)?.coerceAtLeast(0) ?: 0 }.toMutableList()
        val taken = requirements.map { requirement -> take(items, remaining, requirement) }
        val missing = requirements.zip(taken) { requirement, got -> requirement.amount - got }
        return Allocation(remaining.toList(), taken, missing)
    }

    private fun <T> take(items: List<T?>, remaining: MutableList<Int>, requirement: Requirement<T>): Int {
        var got = 0
        for (index in items.indices) {
            if (got == requirement.amount) break
            val item = items[index] ?: continue
            if (remaining[index] <= 0 || !requirement.accepts(item)) continue
            val used = minOf(remaining[index], requirement.amount - got)
            remaining[index] -= used
            got += used
        }
        return got
    }
}
