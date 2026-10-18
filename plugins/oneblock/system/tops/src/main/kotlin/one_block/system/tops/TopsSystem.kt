package one_block.system.tops

import me.devnatan.inventoryframework.ViewFrame
import one_block.system.tops.view.TopsView
import org.bukkit.plugin.java.JavaPlugin

object TopsSystem {

    fun init(plugin: JavaPlugin, viewFrame: ViewFrame) {
        setupListeners(plugin)
        viewFrame.with(TopsView())
    }

    private fun setupListeners(plugin: JavaPlugin) = with(plugin.server.pluginManager) {
    }
}
