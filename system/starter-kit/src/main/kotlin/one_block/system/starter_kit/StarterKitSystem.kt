package one_block.system.starter_kit

import one_block.system.starter_kit.listener.PlayerListener
import org.bukkit.plugin.java.JavaPlugin

object StarterKitSystem {

    fun init(plugin: JavaPlugin) {
        setupListeners(plugin)
    }

    private fun setupListeners(plugin: JavaPlugin) = with(plugin.server.pluginManager) {
        registerEvents(PlayerListener(), plugin)
    }
}