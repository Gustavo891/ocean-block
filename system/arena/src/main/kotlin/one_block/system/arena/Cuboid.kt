package one_block.system.arena

import com.redescreen.sdk.world.BlockPos
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

data class Cuboid(
    val minX: Int,
    val minY: Int,
    val minZ: Int,
    val maxX: Int,
    val maxY: Int,
    val maxZ: Int,
) {

    companion object {
        val ZERO: Cuboid = Cuboid(0, 0, 0, 0, 0, 0)

        fun from(minBlock: Vector, maxBlock: Vector): Cuboid {
            val minX = floor(minBlock.x).toInt().coerceAtMost(floor(maxBlock.x).toInt())
            val minY = floor(minBlock.y).toInt().coerceAtMost(floor(maxBlock.y).toInt())
            val minZ = floor(minBlock.z).toInt().coerceAtMost(floor(maxBlock.z).toInt())

            val maxX = floor(minBlock.x).toInt().coerceAtLeast(floor(maxBlock.x).toInt())
            val maxY = floor(minBlock.y).toInt().coerceAtLeast(floor(maxBlock.y).toInt())
            val maxZ = floor(minBlock.z).toInt().coerceAtLeast(floor(maxBlock.z).toInt())

            return Cuboid(minX, minY, minZ, maxX, maxY, maxZ)
        }

        fun from(minBlock: Location, maxBlock: Location): Cuboid {
            val minX = min(minBlock.blockX, maxBlock.blockX)
            val minY = min(minBlock.blockY, maxBlock.blockY)
            val minZ = min(minBlock.blockZ, maxBlock.blockZ)
            val maxX = max(minBlock.blockX, maxBlock.blockX)
            val maxY = max(minBlock.blockY, maxBlock.blockY)
            val maxZ = max(minBlock.blockZ, maxBlock.blockZ)

            return Cuboid(minX, minY, minZ, maxX, maxY, maxZ)
        }

        fun from(minBlock: BlockPos, maxBlock: BlockPos): Cuboid {
            val minX = min(minBlock.x, maxBlock.x)
            val minY = min(minBlock.y, maxBlock.y)
            val minZ = min(minBlock.z, maxBlock.z)
            val maxX = max(minBlock.x, maxBlock.x)
            val maxY = max(minBlock.y, maxBlock.y)
            val maxZ = max(minBlock.z, maxBlock.z)

            return Cuboid(minX, minY, minZ, maxX, maxY, maxZ)
        }
    }

    val deltaX: Int = maxX - minX
    val deltaY: Int = maxY - minY
    val deltaZ: Int = maxZ - minZ

    val minChunkX: Int
        get() = minX shr 4
    val minChunkY: Int
        get() = minY shr 4
    val minChunkZ: Int
        get() = minZ shr 4
    val maxChunkX: Int
        get() = maxX shr 4
    val maxChunkY: Int
        get() = maxY shr 4
    val maxChunkZ: Int
        get() = maxZ shr 4

    val volume: Int = (deltaX + 1) * (deltaY + 1) * (deltaZ + 1)

    val isZeroed: Boolean = minX == 0 && minY == 0 && minZ == 0 && maxX == 0 && maxY == 0 && maxZ == 0

    val chunkSections: Collection<Vector> = calculateSections(this)

    val positions: Sequence<Vector>
        get() = sequence {
            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    for (z in minZ..maxZ) {
                        yield(Vector(x, y, z))
                    }
                }
            }
        }

    fun containsWithoutY(x: Int, z: Int): Boolean {
        return minX <= x && minZ <= z && maxX >= x && maxZ >= z
    }

    fun contains(x: Int, y: Int, z: Int): Boolean {
        return minX <= x && minY <= y && minZ <= z &&
            maxX >= x && maxY >= y && maxZ >= z
    }

    fun isOnEdge(x: Int, y: Int, z: Int): Boolean {
        return (x == minX || x == maxX) || (y == minY || y == maxY) || (z == minZ || z == maxZ)
    }

    fun intersectsChunk(chunk: Vector): Boolean {
        return minChunkX <= chunk.x && maxChunkX >= chunk.x && minChunkZ <= chunk.y && maxChunkZ >= chunk.y
    }

    fun translate(x: Int, y: Int, z: Int): Cuboid {
        return cuboidOf(
            minX + x,
            minY + y,
            minZ + z,
            maxX + x,
            maxY + y,
            maxZ + z,
        )
    }

    fun randomPosition(): Vector {
        return Vector(
            (minX..maxX).random(),
            (minY..maxY).random(),
            (minZ..maxZ).random(),
        )
    }

    fun isInCylinder(player: Player, centerX: Double, centerZ: Double, radius: Double, minY: Int): Boolean {
        val dx = player.location.x - centerX
        val dz = player.location.z - centerZ
        return dx * dx + dz * dz <= radius * radius && player.location.y >= minY
    }
}

fun cuboidOf(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Cuboid = Cuboid(
    min(x1, x2), min(y1, y2), min(z1, z2),
    max(x1, x2), max(y1, y2), max(z1, z2),
)

private fun calculateSections(cuboid: Cuboid): Collection<Vector> {
    val sections = mutableSetOf<Vector>()

    for (x in (cuboid.minX shr 4)..(cuboid.maxX shr 4)) {
        for (y in (cuboid.minY shr 4)..(cuboid.maxY shr 4)) {
            for (z in (cuboid.minZ shr 4)..(cuboid.maxZ shr 4)) {
                sections += Vector(x, y, z)
            }
        }
    }

    return sections
}