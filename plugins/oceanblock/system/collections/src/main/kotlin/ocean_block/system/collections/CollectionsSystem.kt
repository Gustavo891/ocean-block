package ocean_block.system.collections

import co.aikar.commands.PaperCommandManager
import me.devnatan.inventoryframework.ViewFrame
import ocean_block.system.collections.collection.CollectionManager
import ocean_block.system.collections.command.CollectionCommand
import ocean_block.system.collections.listener.AccountResetListener
import ocean_block.system.collections.listener.CollectionListener
import ocean_block.system.collections.listener.PlayerListener
import ocean_block.system.collections.view.CollectionsView
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
        registerEvents(AccountResetListener(), plugin)
        registerEvents(CollectionListener(), plugin)
    }
}
