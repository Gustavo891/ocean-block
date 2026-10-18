package ocean_block.system.worlds.generator

import org.bukkit.HeightMap
import org.bukkit.block.Biome
import org.bukkit.generator.BiomeProvider
import org.bukkit.generator.ChunkGenerator
import org.bukkit.generator.WorldInfo
import java.util.*

class EmptyChunkGenerator : ChunkGenerator() {

    override fun getBaseHeight(worldInfo: WorldInfo, random: Random, x: Int, z: Int, heightMap: HeightMap): Int {
        return 0
    }

    override fun getDefaultBiomeProvider(worldInfo: WorldInfo): BiomeProvider? {
        return EmptyBiomeProvider()
    }

    private class EmptyBiomeProvider : BiomeProvider() {
        override fun getBiome(
            worldInfo: WorldInfo,
            x: Int,
            y: Int,
            z: Int,
        ): Biome {
            return Biome.PLAINS
        }

        override fun getBiomes(worldInfo: WorldInfo): List<Biome?> {
            return Collections.singletonList(Biome.PLAINS)
        }
    }
}
