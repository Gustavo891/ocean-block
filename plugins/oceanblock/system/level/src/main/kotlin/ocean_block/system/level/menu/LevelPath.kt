package ocean_block.system.level.menu

import ocean_block.system.level.curve.LevelCurve

object LevelPath {

    val SLOTS: List<Int> = listOf(
        0, 1, 10, 19, 28, 37, 38, 39, 30, 21, 12, 3, 4,
        5, 14, 23, 32, 41, 42, 43, 34, 25, 16, 7, 8,
    )

    val PER_PAGE: Int = SLOTS.size

    fun pages(): Int = (LevelCurve.MAX_LEVEL + PER_PAGE - 1) / PER_PAGE

    fun levelAt(page: Int, index: Int): Int = page * PER_PAGE + index + 1

    fun pageOf(level: Int): Int = ((level.coerceIn(1, LevelCurve.MAX_LEVEL) - 1) / PER_PAGE)
}
