package one_block.system.spawners.rules

object StackRules {

    const val MAX_STACK = 128

    data class StackResult(val newStack: Int, val consumed: Int, val leftover: Int)

    fun add(current: Int, offered: Int, max: Int = MAX_STACK): StackResult {
        if (offered <= 0) return StackResult(current, consumed = 0, leftover = maxOf(offered, 0))

        val space = (max - current).coerceAtLeast(0)
        val consumed = minOf(space, offered)
        return StackResult(
            newStack = current + consumed,
            consumed = consumed,
            leftover = offered - consumed,
        )
    }

    data class WithdrawResult(val newStack: Int, val withdrawn: Int)

    fun withdraw(current: Int, requested: Int, inventorySpace: Int): WithdrawResult {
        if (requested <= 0 || inventorySpace <= 0) return WithdrawResult(current, withdrawn = 0)

        val withdrawn = minOf(current, requested, inventorySpace)
        return WithdrawResult(newStack = current - withdrawn, withdrawn = withdrawn)
    }
}
