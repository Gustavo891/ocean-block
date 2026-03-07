package one_block.system.xpbottle

import co.aikar.commands.PaperCommandManager
import one_block.system.xpbottle.command.XpBottleCommand
import one_block.system.xpbottle.listener.XpBottleListener
import org.bukkit.plugin.java.JavaPlugin

object XpBottleSystem {

    fun init(plugin: JavaPlugin, commandManager: PaperCommandManager) {
        setupListeners(plugin)
        setupCommands(commandManager)
    }

    private fun setupListeners(plugin: JavaPlugin) = with(plugin.server.pluginManager) {
         registerEvents(XpBottleListener(), plugin)
    }

    private fun setupCommands(commandManager: PaperCommandManager) {
        commandManager.registerCommand(XpBottleCommand())
    }
}