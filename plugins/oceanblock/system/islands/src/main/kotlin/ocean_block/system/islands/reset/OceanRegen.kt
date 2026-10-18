package ocean_block.system.islands.reset

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import ocean_block.libs.instance.Instance
import org.bukkit.Bukkit

object OceanRegen {

    fun regenerate(area: IslandArea, onDone: (Boolean) -> Unit) {
        Bukkit.getScheduler().runTaskAsynchronously(Instance, Runnable {
            val ok = runCatching {
                val weWorld = BukkitAdapter.adapt(area.world)
                val region = CuboidRegion(
                    weWorld,
                    BlockVector3.at(area.minX, area.minY, area.minZ),
                    BlockVector3.at(area.maxX, area.maxY, area.maxZ),
                )
                WorldEdit.getInstance().newEditSession(weWorld).use { weWorld.regenerate(region, it) }
            }.onFailure {
                Instance.logger.severe("[ilha] Falha no regen da area ${area.minX},${area.minZ}: ${it.message}")
            }.getOrDefault(false)

            Bukkit.getScheduler().runTask(Instance, Runnable { onDone(ok) })
        })
    }
}
