package ocean_block.system.machines.furnace

object FurnaceBar {

    private const val STEPS = 4
    private const val FULL = 100

    fun segmentFill(progress: Int, total: Int, segment: Int, segments: Int): Int {
        if (total <= 0) return 0
        val filled = progress.coerceIn(0, total).toDouble() / total * segments - segment
        val quarters = Math.floor(filled.coerceIn(0.0, 1.0) * STEPS).toInt()
        return quarters * FULL / STEPS
    }
}
