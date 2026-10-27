package ocean_block.system.chest_sort

import org.bukkit.plugin.java.JavaPlugin

object ChestSortSystem {

    fun init(plugin: JavaPlugin) {
        plugin.server.pluginManager.registerEvents(ChestSortListener(), plugin)
    }
}
