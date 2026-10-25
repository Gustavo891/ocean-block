package ocean_block.system.level.notify

class XpTally(private val windowTicks: Long) {

    private var total = 0
    private var lastTick = Long.MIN_VALUE

    fun add(amount: Int, tick: Long): Int {
        total = if (lastTick != Long.MIN_VALUE && tick - lastTick <= windowTicks) total + amount else amount
        lastTick = tick
        return total
    }
}
