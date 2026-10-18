package ocean_block.system.playtime

import ocean_block.system.playtime.expansion.PlayerPlaytimeExpansion
import ocean_block.system.playtime.listener.PlayerListener
import ocean_block.system.playtime.player_playtime.PlayerPlaytimeManager
import org.bukkit.plugin.java.JavaPlugin

object PlaytimeSystem {

    fun init(plugin: JavaPlugin) {
        PlayerPlaytimeManager.onInit()
        setupListeners(plugin)

        PlayerPlaytimeExpansion().register()
    }

    private fun setupListeners(plugin: JavaPlugin) = with(plugin.server.pluginManager) {
        registerEvents(PlayerListener(), plugin)
    }
}
