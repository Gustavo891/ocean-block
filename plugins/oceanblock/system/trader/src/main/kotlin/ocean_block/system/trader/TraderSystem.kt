package ocean_block.system.trader

import me.devnatan.inventoryframework.ViewFrame
import ocean_block.system.trader.listener.TraderListener
import ocean_block.system.trader.view.TraderView
import org.bukkit.plugin.java.JavaPlugin

object TraderSystem {

    lateinit var plugin: JavaPlugin

    fun init(plugin: JavaPlugin, viewFrame: ViewFrame) {
        this.plugin = plugin
        setupListeners(plugin, viewFrame)
        viewFrame.with(TraderView())
    }

    private fun setupListeners(plugin: JavaPlugin, viewFrame: ViewFrame) = with(plugin.server.pluginManager) {
        registerEvents(TraderListener(viewFrame), plugin)
    }

}
