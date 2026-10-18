package ocean_block.system.drift.debris

import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.data.Waterlogged

internal class Column(private val world: World, val x: Int, val z: Int, private val raftY: Int) {

    private val kinds = ColumnKinds { BlockProbe.kind(world, x, it, z) }

    val isLoaded: Boolean get() = DebrisEntities.isLoaded(world, x, z)

    fun kindAt(blockY: Int): BlockKind = kinds.at(blockY)

    fun kindAtDepth(depth: Int): BlockKind = kinds.at(raftY - depth)

    fun isWaterAt(blockY: Int): Boolean = kindAt(blockY) == BlockKind.WATER

    fun isSolidAt(blockY: Int): Boolean = kindAt(blockY) == BlockKind.SOLID
}

internal object BlockProbe {

    private val WATER_TYPES = setOf(
        Material.WATER,
        Material.BUBBLE_COLUMN,
        Material.KELP,
        Material.KELP_PLANT,
        Material.SEAGRASS,
        Material.TALL_SEAGRASS,
    )

    fun kind(world: World, x: Int, y: Int, z: Int): BlockKind {
        val type = world.getType(x, y, z)
        return when {
            !DebrisRules.isPassable(type) -> BlockKind.SOLID
            type in WATER_TYPES -> BlockKind.WATER
            (world.getBlockData(x, y, z) as? Waterlogged)?.isWaterlogged == true -> BlockKind.WATER
            else -> BlockKind.AIR
        }
    }
}
