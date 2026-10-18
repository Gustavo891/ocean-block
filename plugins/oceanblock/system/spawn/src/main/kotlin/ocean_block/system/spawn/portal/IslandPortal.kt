package ocean_block.system.spawn.portal

import ocean_block.libs.instance.Instance
import ocean_block.system.islands.island.IslandManager
import ocean_block.system.spawn.spawn.SpawnManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object IslandPortal {

    private const val INTERVAL_TICKS = 5L

    private const val COOLDOWN_MILLIS = 3000L

    private const val MIN_X = -2
    private const val MIN_Y = 58
    private const val MIN_Z = -9

    private const val MAX_X = 2
    private const val MAX_Y = 58
    private const val MAX_Z = -5

    private val lastUse = ConcurrentHashMap<UUID, Long>()

    private var task: BukkitTask? = null

    fun start() {
        stop()
        task = Bukkit.getScheduler().runTaskTimer(Instance, Runnable { tick() }, INTERVAL_TICKS, INTERVAL_TICKS)
    }

    fun stop() {
        task?.cancel()
        task = null
    }

    fun forget(uuid: UUID) {
        lastUse.remove(uuid)
    }

    fun sendHome(player: Player) {
        val now = System.currentTimeMillis()
        val last = lastUse[player.uniqueId]
        if (last != null && now - last < COOLDOWN_MILLIS) return

        lastUse[player.uniqueId] = now

        val island = IslandManager.getIslandByOwnerUUID(player.uniqueId)
        if (island != null) {
            island.teleportTo(player)
            return
        }

        player.sendRichMessage("<green>Criando sua ilha...")
        IslandManager.createIsland(player)?.teleportTo(player)
    }

    private fun tick() {
        for (player in Bukkit.getOnlinePlayers()) {
            if (!SpawnManager.isAtSpawn(player)) continue
            if (!isInside(player.location)) continue

            sendHome(player)
        }
    }

    private fun isInside(location: Location): Boolean =
        location.blockX in MIN_X..MAX_X &&
            location.blockY in MIN_Y..MAX_Y &&
            location.blockZ in MIN_Z..MAX_Z
}
