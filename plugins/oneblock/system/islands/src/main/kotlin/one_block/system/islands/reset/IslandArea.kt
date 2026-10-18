package one_block.system.islands.reset

import one_block.libs.coordinate.SimpleLocation
import one_block.system.islands.island.Island
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import kotlin.math.floor

data class IslandArea(
    val world: World,
    val minX: Int,
    val maxX: Int,
    val minZ: Int,
    val maxZ: Int,
    val minY: Int,
    val maxY: Int,
) {

    companion object {

        fun of(island: Island): IslandArea? {
            val world = Bukkit.getWorld(island.center.world) ?: return null
            val half = Island.BORDER_SIZE.last() / 2

            val centerX = floor(island.position.originX).toInt()
            val centerZ = floor(island.position.originZ).toInt()

            return IslandArea(
                world = world,
                minX = centerX - half,
                maxX = centerX + half,
                minZ = centerZ - half,
                maxZ = centerZ + half,
                minY = world.minHeight,
                maxY = world.maxHeight - 1,
            )
        }
    }

    fun contains(x: Int, y: Int, z: Int): Boolean =
        x in minX..maxX && z in minZ..maxZ && y in minY..maxY

    fun contains(location: Location): Boolean =
        location.world?.name == world.name &&
            contains(location.blockX, location.blockY, location.blockZ)

    fun contains(location: SimpleLocation): Boolean =
        location.world == world.name &&
            contains(
                floor(location.x).toInt(),
                floor(location.y).toInt(),
                floor(location.z).toInt(),
            )

    fun minChunkX(): Int = minX shr 4

    fun maxChunkX(): Int = maxX shr 4

    fun minChunkZ(): Int = minZ shr 4

    fun maxChunkZ(): Int = maxZ shr 4
}
