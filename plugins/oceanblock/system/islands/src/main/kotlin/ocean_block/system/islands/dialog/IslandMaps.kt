package ocean_block.system.islands.dialog

import ocean_block.system.islands.island.Island
import org.bukkit.Bukkit
import org.bukkit.ChunkSnapshot
import org.bukkit.Location
import org.bukkit.Material
import java.awt.image.BufferedImage
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.floor

object IslandMaps {

    const val SIZE = 64

    private const val CACHE_MILLIS = 20_000L
    private const val MAX_WATER_DEPTH = 24
    private const val SEA_RGB = 0x4040FF

    private class Cached(val grid: List<List<MapColumn>>, val at: Long)

    private val cache = ConcurrentHashMap<UUID, Cached>()

    fun render(island: Island, viewer: Location?): CompletableFuture<BufferedImage> {
        val area = Area.of(island)
        val marker = viewer?.takeIf { it.world?.name == island.center.world }?.let(area::pixel)
        cache[island.uuid]?.takeIf { System.currentTimeMillis() - it.at < CACHE_MILLIS }?.let {
            return CompletableFuture.completedFuture(IslandMapImage.render(it.grid, marker))
        }
        val world = Bukkit.getWorld(island.center.world)
            ?: return CompletableFuture.completedFuture(IslandMapImage.render(sea(), marker))

        val chunks = area.chunks().map { (cx, cz) -> world.getChunkAtAsync(cx, cz).thenApply { (cx to cz) to it.getChunkSnapshot(true, false, false) } }
        return CompletableFuture.allOf(*chunks.toTypedArray())
            .thenApplyAsync {
                val snapshots = chunks.associate { it.join() }
                sample(area, snapshots, world.minHeight)
            }
            .thenApply { grid ->
                cache[island.uuid] = Cached(grid, System.currentTimeMillis())
                IslandMapImage.render(grid, marker)
            }
    }

    private class Area(val minX: Double, val minZ: Double, val size: Double) {

        fun block(pixel: Int, origin: Double): Int = floor(origin + (pixel + 0.5) * size / SIZE).toInt()

        fun chunks(): List<Pair<Int, Int>> {
            val xs = (block(0, minX) shr 4)..(block(SIZE - 1, minX) shr 4)
            val zs = (block(0, minZ) shr 4)..(block(SIZE - 1, minZ) shr 4)
            return xs.flatMap { cx -> zs.map { cz -> cx to cz } }
        }

        fun pixel(location: Location): Pair<Int, Int>? {
            val px = floor((location.x - minX) / size * SIZE).toInt()
            val pz = floor((location.z - minZ) / size * SIZE).toInt()
            return if (px in 0 until SIZE && pz in 0 until SIZE) px to pz else null
        }

        companion object {
            fun of(island: Island): Area {
                val size = island.size.toDouble()
                return Area(island.position.originX - size / 2, island.position.originZ - size / 2, size)
            }
        }
    }

    private fun sample(area: Area, snapshots: Map<Pair<Int, Int>, ChunkSnapshot>, minHeight: Int): List<List<MapColumn>> =
        (0 until SIZE).map { pz ->
            val bz = area.block(pz, area.minZ)
            (0 until SIZE).map { px ->
                val bx = area.block(px, area.minX)
                val snapshot = snapshots[(bx shr 4) to (bz shr 4)]
                if (snapshot == null) MapColumn(SEA_RGB, 0, MAX_WATER_DEPTH) else column(snapshot, bx and 15, bz and 15, minHeight)
            }
        }

    private fun column(snapshot: ChunkSnapshot, x: Int, z: Int, minHeight: Int): MapColumn {
        var y = snapshot.getHighestBlockYAt(x, z)
        while (y > minHeight && snapshot.getBlockType(x, y, z).isAir) y--
        val top = snapshot.getBlockData(x, y, z)
        if (!isWater(top.material)) return MapColumn(top.mapColor.asRGB(), y, 0)

        var depth = 1
        while (depth < MAX_WATER_DEPTH && y - depth > minHeight && isWater(snapshot.getBlockType(x, y - depth, z))) depth++
        return MapColumn(top.mapColor.asRGB(), y, depth)
    }

    private fun isWater(material: Material): Boolean = material in WATER

    private val WATER = setOf(Material.WATER, Material.KELP, Material.KELP_PLANT, Material.SEAGRASS, Material.TALL_SEAGRASS, Material.BUBBLE_COLUMN)

    private fun sea(): List<List<MapColumn>> = List(SIZE) { List(SIZE) { MapColumn(SEA_RGB, 0, MAX_WATER_DEPTH) } }
}
