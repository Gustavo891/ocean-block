package one_block.libs.coordinate

import org.bukkit.Bukkit
import org.bukkit.Location

data class SimpleLocation(
    val x: Double,
    val y: Double,
    val z: Double,

    val yaw: Float = 0f,
    val pitch: Float = 0f,
    val world: String = "world",

    ) {

    companion object {
        fun fromBukkit(location: Location): SimpleLocation {
            return SimpleLocation(
                world = location.world.name,
                x = location.x,
                y = location.y,
                z = location.z,
                yaw = location.yaw,
                pitch = location.pitch
            )
        }
    }

    fun add(x: Double, y: Double, z: Double): SimpleLocation {
        return SimpleLocation(this.x + x, this.y + y, this.z + z)
    }

    fun toBukkit(): Location? {
        val bukkitWorld = Bukkit.getWorld(world) ?: return null
        return Location(bukkitWorld, x, y, z, yaw, pitch)
    }

    fun isSameBlock(location: Location): Boolean {
        return location.world?.name == world &&
            location.blockX == x.toInt() &&
            location.blockY == y.toInt() &&
            location.blockZ == z.toInt()
    }
}
