package ocean_block.system.research

import ocean_block.system.craft.recipe.RecipeLocks
import ocean_block.system.islands.island.IslandManager
import ocean_block.system.research.data.ResearchManager
import ocean_block.system.research.tree.ResearchTree
import one_block.textura.api.NotificationApi
import org.bukkit.plugin.java.JavaPlugin

object ResearchSystem {

    fun init(plugin: JavaPlugin) {
        ResearchManager.onInit()
        RecipeLocks.install { player, recipe ->
            val island = IslandManager.getIslandByPlayer(player.uniqueId)
            ResearchLocks.reason(recipe, island?.let { ResearchManager.completed(it.uuid) })
        }
        ResearchTimer.start(plugin)
        NotificationApi.registerItemIcons(ResearchTree.all.map { it.icon.removePrefix("item:") }.distinct())
    }

    fun onDisable() {
        ResearchManager.onDisable()
    }
}
