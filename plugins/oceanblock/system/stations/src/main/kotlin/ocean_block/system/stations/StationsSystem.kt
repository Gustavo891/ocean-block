package ocean_block.system.stations

import ocean_block.system.custom_items.block.CustomBlockRegistry
import ocean_block.system.stations.item.VanillaGuard
import ocean_block.system.stations.recipe.StationRecipes
import ocean_block.system.stations.smelter.SmelterBlock
import ocean_block.system.stations.smelter.SmelterDisplay
import ocean_block.system.stations.smelter.SmelterListener
import org.bukkit.plugin.java.JavaPlugin

object StationsSystem {

    fun init(plugin: JavaPlugin) {
        SmelterBlock.all.forEach(CustomBlockRegistry::register)
        plugin.server.pluginManager.registerEvents(SmelterListener(), plugin)
        plugin.server.pluginManager.registerEvents(VanillaGuard(), plugin)
        StationRecipes.registerSmelters()
    }

    fun onDisable() {
        SmelterDisplay.removeAll()
    }
}
