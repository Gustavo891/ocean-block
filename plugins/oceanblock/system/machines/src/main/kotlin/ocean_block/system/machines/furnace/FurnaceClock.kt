package ocean_block.system.machines.furnace

object FurnaceClock {

    private const val TICKS_PER_SECOND = 20

    fun nextSeconds(progress: Int, ticksPerItem: Int): Int =
        (ticksPerItem - progress.coerceIn(0, ticksPerItem) + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND

    fun totalSeconds(progress: Int, queued: Int, ticksPerItem: Int): Int =
        if (queued <= 0) 0 else nextSeconds(progress, ticksPerItem) + (queued - 1) * ticksPerItem / TICKS_PER_SECOND

    fun format(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)
}
