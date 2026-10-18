package ocean_block.system.drift.debris

import ocean_block.system.drift.config.DebrisType
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f

internal object DebrisEntities {

    private const val TELEPORT_TICKS = 2
    private const val INTERPOLATION_TICKS = 2
    private const val CHUNK_SHIFT = 4

    fun isLoaded(world: World, blockX: Int, blockZ: Int): Boolean =
        world.isChunkLoaded(blockX shr CHUNK_SHIFT, blockZ shr CHUNK_SHIFT)

    fun spawn(world: World, pose: Pose, type: DebrisType): ItemDisplay =
        world.spawn(Location(world, pose.point.x, pose.y, pose.point.z), ItemDisplay::class.java) {
            it.setItemStack(ItemStack(type.display))
            it.isPersistent = false
            it.teleportDuration = TELEPORT_TICKS
            it.transformation = posed(it.transformation, pose.rotation, Vector3f(type.scale, type.scale, type.scale))
        }

    fun place(entity: ItemDisplay, pose: Pose, scale: Float? = null) {
        entity.teleport(Location(entity.world, pose.point.x, pose.y, pose.point.z))
        entity.interpolationDelay = 0
        entity.interpolationDuration = INTERPOLATION_TICKS
        val current = entity.transformation
        entity.transformation = posed(current, pose.rotation, scale?.let { Vector3f(it, it, it) } ?: current.scale)
    }

    private fun posed(base: Transformation, rotation: Quaternionf, scale: Vector3f): Transformation =
        Transformation(base.translation, rotation, scale, base.rightRotation)
}
