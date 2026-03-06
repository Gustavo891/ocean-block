package one_block.system.fly_timer

import co.aikar.commands.PaperCommandManager
import one_block.libs.instance.Instance
import one_block.system.fly_timer.command.FlyCommand
import one_block.system.fly_timer.fly_player.FlyPlayerManager
import one_block.system.fly_timer.listener.PlayerListener

object FlyTimerSystem {

    fun init(paperCommandManager: PaperCommandManager) {
        FlyPlayerManager.onInit()

        setupListeners()
        setupCommands(paperCommandManager)
    }

    private fun setupListeners() = with(Instance.server.pluginManager) {
        registerEvents(PlayerListener(), Instance)
    }

    private fun setupCommands(paperCommandManager: PaperCommandManager) {
        paperCommandManager.registerCommand(FlyCommand())
    }
}