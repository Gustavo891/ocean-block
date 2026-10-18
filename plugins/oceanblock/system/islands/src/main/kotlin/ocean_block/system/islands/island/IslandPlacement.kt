package ocean_block.system.islands.island

import ocean_block.libs.coordinate.SimpleLocation
import ocean_block.system.islands.position.Position

object IslandPlacement {
    const val PASTE_Y = 64
    const val GROUND_Y = 63
    const val FALLBACK_SIZE = 3

    fun center(position: Position, world: String): SimpleLocation =
        SimpleLocation(position.originX, GROUND_Y.toDouble(), position.originZ, world = world)

    fun spawn(position: Position, world: String): SimpleLocation =
        SimpleLocation(position.originX + 0.5, PASTE_Y.toDouble(), position.originZ + 0.5, world = world)

    fun fallbackFootprint(centerX: Int, centerZ: Int): List<Pair<Int, Int>> {
        val radius = FALLBACK_SIZE / 2
        return (-radius..radius).flatMap { dx ->
            (-radius..radius).map { dz -> (centerX + dx) to (centerZ + dz) }
        }
    }
}
