package one_block.system.chats

import one_block.system.chats.listener.PlayerListener
import org.bukkit.plugin.java.JavaPlugin

object ChatsSystem {

    fun init(plugin: JavaPlugin) {
        setupListeners(plugin)
    }

    private fun setupListeners(plugin: JavaPlugin) = with(plugin.server.pluginManager) {
        registerEvents(PlayerListener(), plugin)
    }
}