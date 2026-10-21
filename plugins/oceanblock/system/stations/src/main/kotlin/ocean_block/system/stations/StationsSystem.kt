package ocean_block.system.stations

import ocean_block.system.custom_items.item.CustomItemRegistry
import ocean_block.system.stations.config.StationsFile
import ocean_block.system.stations.item.OrePiece
import ocean_block.system.stations.sieve.SieveTable
import org.bukkit.plugin.java.JavaPlugin

object StationsSystem {

    lateinit var sieveTable: SieveTable
        private set

    fun init(plugin: JavaPlugin) {
        OrePiece.all.forEach(CustomItemRegistry::register)
        sieveTable = StationsFile.loadSieve(plugin)
    }
}
