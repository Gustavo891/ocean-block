package ocean_block.system.craft

import co.aikar.commands.PaperCommandManager
import me.devnatan.inventoryframework.ViewFrame
import ocean_block.system.craft.command.ReceitasCommand
import ocean_block.system.craft.menu.RecipesMenu
import ocean_block.system.craft.menu.RecipesQuitListener
import org.bukkit.plugin.java.JavaPlugin

object CraftSystem {

    fun init(plugin: JavaPlugin, viewFrame: ViewFrame, paperCommandManager: PaperCommandManager) {
        viewFrame.with(RecipesMenu())
        paperCommandManager.registerCommand(ReceitasCommand(viewFrame))
        plugin.server.pluginManager.registerEvents(RecipesQuitListener(), plugin)
    }
}
