package one_block.system.islands.reset

import one_block.libs.instance.Instance
import org.bukkit.Material
import org.bukkit.scheduler.BukkitRunnable

object TerrainCleaner {

    private const val BLOCKS_PER_TICK = 20_000

    private val air = Material.AIR.createBlockData()

    fun count(area: IslandArea): Int {
        var total = 0

        for (x in area.minX..area.maxX) {
            for (z in area.minZ..area.maxZ) {
                val top = area.world.getHighestBlockYAt(x, z)

                for (y in area.minY..minOf(top, area.maxY)) {
                    if (area.world.getBlockAt(x, y, z).type != Material.AIR) total++
                }
            }
        }

        return total
    }

    fun clear(area: IslandArea, onProgress: (Int) -> Unit, onDone: (Int) -> Unit) {
        val columns = ArrayDeque<Pair<Int, Int>>()

        for (x in area.minX..area.maxX) {
            for (z in area.minZ..area.maxZ) {
                columns.addLast(x to z)
            }
        }

        val totalColumns = columns.size

        object : BukkitRunnable() {

            private var removed = 0
            private var lastReport = 0

            override fun run() {
                var budget = BLOCKS_PER_TICK

                while (budget > 0 && columns.isNotEmpty()) {
                    val (x, z) = columns.removeFirst()
                    val top = minOf(area.world.getHighestBlockYAt(x, z), area.maxY)

                    for (y in area.minY..top) {
                        budget--
                        val block = area.world.getBlockAt(x, y, z)
                        if (block.type == Material.AIR) continue

                        block.setBlockData(air, false)
                        removed++
                    }
                }

                if (columns.isEmpty()) {
                    cancel()
                    onDone(removed)
                    return
                }

                val percent = ((totalColumns - columns.size) * 100) / totalColumns
                if (percent >= lastReport + 10) {
                    lastReport = percent
                    onProgress(percent)
                }
            }
        }.runTaskTimer(Instance, 1L, 1L)
    }
}
