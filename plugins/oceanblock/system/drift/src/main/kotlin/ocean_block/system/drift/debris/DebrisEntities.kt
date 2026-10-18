package ocean_block.system.drift.debris

import ocean_block.system.drift.config.DebrisType
import ocean_block.system.drift.lane.LanePoint
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Transformation
import org.joml.Vector3f

object DebrisEntities {

    private const val TELEPORT_TICKS = 2
    private const val CHUNK_SHIFT = 4

    fun isLoaded(world: World, blockX: Int, blockZ: Int): Boolean =
        world.isChunkLoaded(blockX shr CHUNK_SHIFT, blockZ shr CHUNK_SHIFT)

    fun spawn(world: World, point: LanePoint, y: Double, type: DebrisType): ItemDisplay =
        world.spawn(Location(world, point.x, y, point.z), ItemDisplay::class.java) {
            it.setItemStack(ItemStack(type.display))
            it.isPersistent = false
            it.teleportDuration = TELEPORT_TICKS
            it.transformation = scaled(it.transformation, type.scale)
        }

    private fun scaled(base: Transformation, scale: Float): Transformation =
        Transformation(base.translation, base.leftRotation, Vector3f(scale, scale, scale), base.rightRotation)
}
