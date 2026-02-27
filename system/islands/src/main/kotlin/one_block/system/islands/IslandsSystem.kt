package one_block.system.islands

import me.devnatan.inventoryframework.ViewFrame
import one_block.system.islands.expansion.IslandExpansion
import one_block.system.islands.island.IslandManager
import one_block.system.islands.phase.PhaseManager
import one_block.system.islands.view.IslandsView
import one_block.system.islands.view.PhaseView
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

object IslandsSystem {

    fun init(plugin: JavaPlugin, viewFrame: ViewFrame) {
        Bukkit.getScheduler().runTaskLater(plugin, Runnable { IslandManager.onInit() }, 20L)
        PhaseManager.loadPhases()
        viewFrame.with(PhaseView(), IslandsView())

        IslandExpansion().register()
    }
}