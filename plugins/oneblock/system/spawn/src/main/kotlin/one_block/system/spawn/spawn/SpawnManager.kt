package one_block.system.spawn.spawn

import one_block.libs.coordinate.SimpleLocation
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player

object SpawnManager {

    private const val PATH = "spawn"

    @Volatile
    private var spawn: SimpleLocation? = null

    fun onInit() {
        SpawnFile.setup()
        spawn = read()
    }

    fun location(): SimpleLocation? = spawn

    fun isDefined(): Boolean = spawn != null

    fun set(location: Location) {
        val simple = SimpleLocation.fromBukkit(location)
        spawn = simple
        write(simple)
    }

    fun isSpawnWorld(world: World?): Boolean {
        val current = spawn ?: return false
        return world != null && world.name == current.world
    }

    fun isAtSpawn(player: Player): Boolean = isSpawnWorld(player.world)

    fun teleport(player: Player): Boolean {
        val target = spawn?.toBukkit() ?: return false
        player.teleport(target)
        return true
    }

    private fun read(): SimpleLocation? {
        val config = SpawnFile.config()
        val world = config.getString("$PATH.world") ?: return null

        return SimpleLocation(
            world = world,
            x = config.getDouble("$PATH.x"),
            y = config.getDouble("$PATH.y"),
            z = config.getDouble("$PATH.z"),
            yaw = config.getDouble("$PATH.yaw").toFloat(),
            pitch = config.getDouble("$PATH.pitch").toFloat(),
        )
    }

    private fun write(location: SimpleLocation) {
        val config = SpawnFile.config()

        config.set("$PATH.world", location.world)
        config.set("$PATH.x", location.x)
        config.set("$PATH.y", location.y)
        config.set("$PATH.z", location.z)
        config.set("$PATH.yaw", location.yaw.toDouble())
        config.set("$PATH.pitch", location.pitch.toDouble())

        SpawnFile.save()
    }
}
