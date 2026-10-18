package one_block.system.collections

import co.aikar.commands.PaperCommandManager
import me.devnatan.inventoryframework.ViewFrame
import one_block.system.collections.collection.CollectionManager
import one_block.system.collections.command.CollectionCommand
import one_block.system.collections.listener.CollectionListener
import one_block.system.collections.listener.PlayerListener
import one_block.system.collections.view.CollectionsView
import org.bukkit.plugin.java.JavaPlugin

object CollectionsSystem {

    fun init(plugin: JavaPlugin, viewFrame: ViewFrame, commandManager: PaperCommandManager) {
        CollectionManager.onInit()

        viewFrame.with(CollectionsView())

        commandManager.registerCommand(CollectionCommand(viewFrame))

        setupListeners(plugin)
    }

    fun onDisable() = CollectionManager.onDisable()

    private fun setupListeners(plugin: JavaPlugin) = with(plugin.server.pluginManager) {
        registerEvents(PlayerListener(), plugin)
        registerEvents(CollectionListener(), plugin)
    }
}
