package one_block.trader.task

import one_block.libs.instance.Instance
import one_block.system.islands.island.IslandManager
import one_block.trader.manager.TraderManager
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import java.util.*
import java.util.concurrent.ThreadLocalRandom

class TraderTask : BukkitRunnable() {

    private val taskInterval = 20L * 10L
    private val islandFailedAttempts = mutableMapOf<UUID, Int>()

    init {
        runTaskTimer(Instance, taskInterval, taskInterval)
    }

    override fun run() {
        val onlinePlayers = Bukkit.getOnlinePlayers().toList()
        if (onlinePlayers.isEmpty()) return

        val randomPlayer = onlinePlayers.random()
        val island = IslandManager.getIslandByPlayer(randomPlayer.uniqueId) ?: return
        if (TraderManager.hasTrader(island.uuid)) return

        val failedAttempts = islandFailedAttempts.getOrDefault(island.uuid, 0)
        val chance = spawnChance(failedAttempts)
        val rolled = ThreadLocalRandom.current().nextDouble(100.0)

        if (rolled < chance) {
            TraderManager.spawnTrader(island)
            islandFailedAttempts[island.uuid] = 0
        } else {
            islandFailedAttempts[island.uuid] = failedAttempts + 1
        }
    }

    private fun spawnChance(failedAttempts: Int): Double = when (failedAttempts) {
        0 -> 25.0
        1 -> 50.0
        else -> 75.0
    }
}