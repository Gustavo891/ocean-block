package ocean_block.system.worlds

import ocean_block.system.worlds.manager.WorldManager
import ocean_block.system.worlds.utils.WorldFile
import org.bukkit.plugin.java.JavaPlugin

object WorldsSystem {

    fun init(plugin: JavaPlugin) {
        WorldFile.setup()
        WorldManager.load()

        setupListeners(plugin)
    }

    private fun setupListeners(plugin: JavaPlugin) = with(plugin.server.pluginManager) {
    }
}
