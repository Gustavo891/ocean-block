package one_block.system.trader.task

import one_block.libs.instance.Instance
import one_block.system.islands.island.IslandManager
import one_block.system.trader.manager.TraderManager
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable

class TraderDespawnTask : BukkitRunnable() {

    private val checkInterval = 100L
    private val maxTime = 60 * 1000L

    init {
        runTaskTimer(Instance, 0L, checkInterval)
    }

    override fun run() {
        val now = System.currentTimeMillis()

        TraderManager.activeTraders.toMap().forEach { (traderEntityId, activeTrader) ->
            val entity = Bukkit.getEntity(traderEntityId)

            val isDead = entity == null || entity.isDead
            val isExpired = now >= activeTrader.spawnTime + maxTime

            val island = IslandManager.islandsById[activeTrader.islandId] ?: return@forEach

            when {
                isDead -> {
                    TraderManager.traderRemove(traderEntityId, island)
                }

                isExpired -> {
                    entity.remove()
                    TraderManager.traderRemove(traderEntityId, island)
                }
            }
        }
    }


}
