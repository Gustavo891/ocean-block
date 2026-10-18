package ocean_block.system.scoreboard

import co.aikar.commands.PaperCommandManager
import ocean_block.system.scoreboard.board.BoardManager
import ocean_block.system.scoreboard.command.ScoreboardCommand
import ocean_block.system.scoreboard.config.ScoreboardConfig
import ocean_block.system.scoreboard.listener.ScoreboardListener
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
