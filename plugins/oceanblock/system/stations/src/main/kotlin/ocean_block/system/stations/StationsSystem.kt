package ocean_block.system.stations

import ocean_block.system.custom_items.block.CustomBlockRegistry
import ocean_block.system.custom_items.item.CustomItemRegistry
import ocean_block.system.stations.config.StationsFile
import ocean_block.system.stations.item.OrePiece
import ocean_block.system.stations.item.VanillaGuard
import ocean_block.system.stations.recipe.StationRecipes
import ocean_block.system.stations.sieve.SieveBlock
import ocean_block.system.stations.sieve.SieveDelivery
import ocean_block.system.stations.sieve.SieveListener
import ocean_block.system.stations.sieve.SieveTable
import ocean_block.system.stations.smelter.SmelterBlock
import ocean_block.system.stations.smelter.SmelterDisplay
import ocean_block.system.stations.smelter.SmelterListener
import one_block.textura.api.NotificationApi
import org.bukkit.plugin.java.JavaPlugin

object StationsSystem {

    lateinit var sieveTable: SieveTable
        private set

    fun init(plugin: JavaPlugin) {
        OrePiece.all.forEach(CustomItemRegistry::register)
        sieveTable = StationsFile.loadSieve(plugin)

        SieveBlock.all.forEach(CustomBlockRegistry::register)
        plugin.server.pluginManager.registerEvents(SieveListener(), plugin)
        StationRecipes.registerSieves()

        SmelterBlock.all.forEach(CustomBlockRegistry::register)
        plugin.server.pluginManager.registerEvents(SmelterListener(), plugin)
        plugin.server.pluginManager.registerEvents(VanillaGuard(), plugin)
        StationRecipes.registerSmelters()

        NotificationApi.registerItemIcons(SieveDelivery.icons(sieveTable.outputs()))
    }

    fun onDisable() {
        SmelterDisplay.removeAll()
    }
}
