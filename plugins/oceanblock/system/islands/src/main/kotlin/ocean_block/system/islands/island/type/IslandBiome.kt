package ocean_block.system.islands.island.type

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.world.biome.BiomeTypes
import ocean_block.libs.instance.Instance
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.reset.IslandArea
import org.bukkit.Bukkit

object IslandBiome {

    private const val STEP = 4

    fun paint(island: Island) {
        val id = island.type().biome ?: return
        val area = IslandArea.of(island) ?: return
        val biome = BiomeTypes.get("minecraft:$id")

        if (biome == null) {
            Instance.logger.warning("[ilha] Bioma '$id' não existe; ilha ${island.uuid} fica com o bioma do mundo.")
            return
        }

        Bukkit.getScheduler().runTaskAsynchronously(Instance, Runnable {
            val result = runCatching {
                WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(area.world)).use { edit ->
                    for (x in area.minX..area.maxX step STEP) {
                        for (z in area.minZ..area.maxZ step STEP) {
                            for (y in area.minY..area.maxY step STEP) {
                                edit.setBiome(x, y, z, biome)
                            }
                        }
                    }
                }
            }

            Bukkit.getScheduler().runTask(Instance, Runnable {
                result.onFailure { Instance.logger.severe("[ilha] Falha ao pintar o bioma da ilha ${island.uuid}: ${it.message}") }
                refresh(area)
            })
        })
    }

    private fun refresh(area: IslandArea) {
        for (chunkX in area.minChunkX()..area.maxChunkX()) {
            for (chunkZ in area.minChunkZ()..area.maxChunkZ()) {
                if (area.world.isChunkLoaded(chunkX, chunkZ)) area.world.refreshChunk(chunkX, chunkZ)
            }
        }
    }
}
