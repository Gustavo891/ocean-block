package one_block.system.mobdrops

import co.aikar.commands.PaperCommandManager
import me.devnatan.inventoryframework.ViewFrame
import one_block.libs.instance.Instance
import one_block.system.mobdrops.command.MobDropsCommand
import one_block.system.mobdrops.listener.MobDropListener
import one_block.system.mobdrops.manager.MobDropManager

object MobDropsSystem {

    fun init(viewFrame: ViewFrame, paperCommandManager: PaperCommandManager) {
        MobDropManager.onInit()

        setupCommands(viewFrame, paperCommandManager)
        setupListeners()
    }

    private fun setupCommands(viewFrame: ViewFrame, commandManager: PaperCommandManager) {
        commandManager.registerCommand(MobDropsCommand())
    }

    private fun setupListeners() = with(Instance.server.pluginManager){
        registerEvents(MobDropListener(), Instance)
    }
}
