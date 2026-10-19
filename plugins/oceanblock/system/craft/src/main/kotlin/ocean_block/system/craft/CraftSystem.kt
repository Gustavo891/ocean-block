package ocean_block.system.craft

import co.aikar.commands.PaperCommandManager
import ocean_block.system.craft.command.ReceitasCommand
import ocean_block.system.craft.screen.RecipesQuitListener
import ocean_block.system.craft.screen.RecipesScreen
import org.bukkit.plugin.java.JavaPlugin

object CraftSystem {

    fun init(plugin: JavaPlugin, paperCommandManager: PaperCommandManager) {
        val screen = RecipesScreen(plugin.logger)
        paperCommandManager.registerCommand(ReceitasCommand(screen))
        plugin.server.pluginManager.registerEvents(RecipesQuitListener(screen), plugin)
        plugin.server.scheduler.runTask(plugin, Runnable { screen.registerIcons() })
    }
}
