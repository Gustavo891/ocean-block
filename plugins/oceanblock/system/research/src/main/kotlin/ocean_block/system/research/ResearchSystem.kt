package ocean_block.system.research

import co.aikar.commands.PaperCommandManager
import ocean_block.system.craft.recipe.RecipeLocks
import ocean_block.system.islands.island.IslandManager
import ocean_block.system.research.command.PesquisaCommand
import ocean_block.system.research.data.ResearchManager
import ocean_block.system.research.screen.ResearchQuitListener
import ocean_block.system.research.tree.ResearchTree
import one_block.textura.api.DialogApi
import one_block.textura.api.NotificationApi
import org.bukkit.plugin.java.JavaPlugin

object ResearchSystem {

    fun init(plugin: JavaPlugin, paperCommandManager: PaperCommandManager) {
        ResearchManager.onInit()
        RecipeLocks.install { player, recipe ->
            val island = IslandManager.getIslandByPlayer(player.uniqueId)
            ResearchLocks.reason(recipe, island?.let { ResearchManager.completed(it.uuid) })
        }
        paperCommandManager.registerCommand(PesquisaCommand())
        plugin.server.pluginManager.registerEvents(ResearchQuitListener(), plugin)
        ResearchTimer.start(plugin)
        NotificationApi.registerItemIcons(ResearchTree.iconMaterials())
        DialogApi.registerItemIcons(ResearchTree.iconMaterials())
    }

    fun onDisable() {
        ResearchManager.onDisable()
    }
}
