package one_block.system.chats

import org.bukkit.plugin.java.JavaPlugin

object ChatsSystem {

    fun init(plugin: JavaPlugin) {
        setupListeners(plugin)
    }

    private fun setupListeners(plugin: JavaPlugin) = with(plugin.server.pluginManager) {
    }
}
