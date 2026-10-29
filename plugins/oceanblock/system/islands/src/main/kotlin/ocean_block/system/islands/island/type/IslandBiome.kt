package ocean_block.system.islands.island.type

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import ocean_block.libs.instance.Instance
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.reset.IslandArea
import org.bukkit.NamespacedKey

object IslandBiome {

    private const val STEP = 4

    fun paint(island: Island) {
        val id = island.type().biome ?: return
        val area = IslandArea.of(island) ?: return
        val biome = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).get(NamespacedKey.minecraft(id))

        if (biome == null) {
            Instance.logger.warning("[ilha] Bioma '$id' não existe; ilha ${island.uuid} fica com o bioma do mundo.")
            return
        }

        for (x in area.minX..area.maxX step STEP) {
            for (z in area.minZ..area.maxZ step STEP) {
                for (y in area.minY..area.maxY step STEP) {
                    area.world.setBiome(x, y, z, biome)
                }
            }
        }

        for (chunkX in area.minChunkX()..area.maxChunkX()) {
            for (chunkZ in area.minChunkZ()..area.maxChunkZ()) {
                if (area.world.isChunkLoaded(chunkX, chunkZ)) area.world.refreshChunk(chunkX, chunkZ)
            }
        }
    }
}
