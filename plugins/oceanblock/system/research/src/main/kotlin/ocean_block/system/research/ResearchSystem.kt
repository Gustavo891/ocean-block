package ocean_block.system.research

import ocean_block.system.craft.recipe.RecipeLocks
import ocean_block.system.custom_items.block.CustomBlockRegistry
import ocean_block.system.islands.island.IslandManager
import ocean_block.system.research.data.ResearchManager
import ocean_block.system.research.table.ResearchTableBlock
import ocean_block.system.research.table.ResearchTableListener
import ocean_block.system.research.tree.ResearchTree
import one_block.textura.api.DialogApi
import one_block.textura.api.NotificationApi
import org.bukkit.plugin.java.JavaPlugin

object ResearchSystem {

    fun init(plugin: JavaPlugin) {
        ResearchManager.onInit()
        RecipeLocks.install { player, recipe ->
            val island = IslandManager.getIslandByPlayer(player.uniqueId)
            ResearchLocks.reason(recipe, island?.let { ResearchManager.completed(it.uuid) })
        }
        CustomBlockRegistry.register(ResearchTableBlock)
        plugin.server.pluginManager.registerEvents(ResearchTableListener(), plugin)
        ResearchRecipes.register()
        ResearchTimer.start(plugin)
        NotificationApi.registerItemIcons(ResearchTree.iconMaterials())
        DialogApi.registerItemIcons(ResearchTree.iconMaterials())
    }

    fun onDisable() {
        ResearchManager.onDisable()
    }
}
