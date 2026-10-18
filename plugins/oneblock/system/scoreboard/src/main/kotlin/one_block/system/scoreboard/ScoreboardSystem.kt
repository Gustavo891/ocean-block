package one_block.system.scoreboard

import co.aikar.commands.PaperCommandManager
import one_block.system.scoreboard.board.BoardManager
import one_block.system.scoreboard.command.ScoreboardCommand
import one_block.system.scoreboard.config.ScoreboardConfig
import one_block.system.scoreboard.listener.ScoreboardListener
import org.bukkit.plugin.java.JavaPlugin

object ScoreboardSystem {

    fun init(plugin: JavaPlugin, commandManager: PaperCommandManager) {
        ScoreboardConfig.load()

        commandManager.registerCommand(ScoreboardCommand())
        plugin.server.pluginManager.registerEvents(ScoreboardListener(), plugin)

        BoardManager.start()
    }

    fun onDisable() {
        BoardManager.stop()
    }
}
