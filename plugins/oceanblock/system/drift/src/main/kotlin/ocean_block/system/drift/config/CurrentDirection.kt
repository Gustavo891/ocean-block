package ocean_block.system.drift.config

enum class CurrentDirection(val dx: Int, val dz: Int) {
    EAST(1, 0),
    WEST(-1, 0),
    SOUTH(0, 1),
    NORTH(0, -1),
}
