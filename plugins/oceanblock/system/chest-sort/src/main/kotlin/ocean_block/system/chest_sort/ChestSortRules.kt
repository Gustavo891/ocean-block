package ocean_block.system.chest_sort

object ChestSortRules {

    private class Group<T>(val sample: T, var total: Int, var largest: Int)

    fun <T> sort(slots: List<T?>, kind: StackKind<T>): List<T?> {
        val groups = group(slots, kind)
        val ordered = groups.sortedWith(
            compareByDescending<Group<T>> { it.total }.thenBy { kind.name(it.sample) }
        )
        val stacks = ordered.flatMap { split(it, kind) }
        return stacks + List(slots.size - stacks.size) { null }
    }

    private fun <T> group(slots: List<T?>, kind: StackKind<T>): List<Group<T>> {
        val groups = mutableListOf<Group<T>>()
        for (stack in slots) {
            if (stack == null) continue
            val amount = kind.amount(stack)
            if (amount <= 0) continue
            val existing = groups.firstOrNull { kind.same(it.sample, stack) }
            if (existing == null) {
                groups += Group(stack, amount, amount)
            } else {
                existing.total += amount
                existing.largest = maxOf(existing.largest, amount)
            }
        }
        return groups
    }

    private fun <T> split(group: Group<T>, kind: StackKind<T>): List<T> {
        val cap = maxOf(kind.maxStack(group.sample), group.largest, 1)
        val full = group.total / cap
        val rest = group.total % cap
        val stacks = List(full) { kind.withAmount(group.sample, cap) }
        return if (rest > 0) stacks + kind.withAmount(group.sample, rest) else stacks
    }
}
