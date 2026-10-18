package one_block.system.minions.behavior

import one_block.system.minions.placed_minion.PlacedMinion
import org.bukkit.Location
import org.bukkit.block.BlockFace
import kotlin.math.atan2

data class MinionPlacement(val location: Location, val facing: BlockFace?)

object MinionFacing {

    val DEFAULT: BlockFace = BlockFace.DOWN

    fun of(placedMinion: PlacedMinion): BlockFace =
        placedMinion.facing?.let { runCatching { BlockFace.valueOf(it) }.getOrNull() } ?: DEFAULT

    fun yawTowards(from: Location, to: Location): Float {
        val dx = to.x - from.x
        val dz = to.z - from.z
        if (dx == 0.0 && dz == 0.0) return from.yaw
        return Math.toDegrees(atan2(-dx, dz)).toFloat()
    }

    fun yaw(face: BlockFace): Float = when (face) {
        BlockFace.SOUTH -> 0.0f
        BlockFace.WEST -> 90.0f
        BlockFace.NORTH -> 180.0f
        BlockFace.EAST -> -90.0f
        else -> 0.0f
    }
}
