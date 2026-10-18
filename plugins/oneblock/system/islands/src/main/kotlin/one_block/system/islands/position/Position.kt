package one_block.system.islands.position

import kotlin.math.ceil
import kotlin.math.sqrt

data class Position(
    val index: Int,
    val gridX: Int,
    val gridZ: Int,
    val originX: Double,
    val originZ: Double,
) {

    companion object {
        const val GRID_SPACING = 1000

        fun fromIndex(index: Int): Position {
            if (index == 0) {
                return create(index, 0, 0)
            }

            val layer = ceil((sqrt(index.toDouble()) - 1) / 2).toInt()
            val legLength = layer * 2
            val legStart = (2 * layer - 1) * (2 * layer - 1)
            val offset = index - legStart

            val (x, z) = when {
                offset < legLength -> layer to (-layer + 1 + offset)
                offset < legLength * 2 -> (layer - 1 - (offset - legLength)) to layer
                offset < legLength * 3 -> -layer to (layer - 1 - (offset - legLength * 2))
                else -> (-layer + 1 + (offset - legLength * 3)) to -layer
            }

            return create(index, x, z)
        }

        private fun create(index: Int, gridX: Int, gridZ: Int): Position {
            val originX = gridX * GRID_SPACING.toDouble()
            val originZ = gridZ * GRID_SPACING.toDouble()
            return Position(index, gridX, gridZ, originX, originZ)
        }
    }
}
