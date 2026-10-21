package ocean_block.system.research

import ocean_block.system.islands.island.IslandManager
import ocean_block.system.research.data.ResearchManager
import ocean_block.system.research.rules.ResearchRules
import ocean_block.system.research.tree.ResearchTree
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

object ResearchTimer {

    private const val PERIOD_TICKS = 20L

    fun start(plugin: JavaPlugin) {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable { ResearchManager.withRunning().forEach(::completeDue) }, PERIOD_TICKS, PERIOD_TICKS)
    }

    fun completeDue(islandUuid: UUID) {
        val done = ResearchManager.edit(islandUuid) { ResearchRules.completeDue(it, System.currentTimeMillis(), ResearchTree.byId) }
        if (done.isEmpty()) return
        ResearchManager.saveNow(islandUuid)
        val island = IslandManager.islandsById[islandUuid] ?: return
        done.mapNotNull(ResearchTree.byId::get).forEach { ResearchNotifier.completed(island, it) }
    }
}
