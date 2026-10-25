package ocean_block.system.custom_items.block.furniture

data class FurnitureHit(val count: Int, val lastTick: Int)

object FurnitureHits {

    const val TO_BREAK = 3
    const val WINDOW_TICKS = 30

    fun next(previous: FurnitureHit?, now: Int): Int =
        if (previous != null && now - previous.lastTick <= WINDOW_TICKS) previous.count + 1 else 1
}
