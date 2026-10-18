package one_block.system.islands.phase

object PhaseProgress {

    fun isComplete(grid: Int, target: Int): Boolean = grid >= target

    fun increment(grid: Int, target: Int): Int = if (isComplete(grid, target)) grid else grid + 1

    fun crossedThreshold(before: Int, after: Int, target: Int): Boolean =
        !isComplete(before, target) && isComplete(after, target)
}
