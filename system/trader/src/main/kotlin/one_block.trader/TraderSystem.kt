package one_block.trader

import me.devnatan.inventoryframework.ViewFrame
import one_block.trader.listener.TraderListener
import one_block.trader.task.TraderDespawnTask
import one_block.trader.task.TraderTask
import one_block.trader.view.TraderView
import org.bukkit.plugin.java.JavaPlugin

object TraderSystem {

    lateinit var plugin: JavaPlugin

    fun init(plugin: JavaPlugin, viewFrame: ViewFrame) {
        this.plugin = plugin
        setupListeners(plugin, viewFrame)
     //   TraderTask()
       // TraderDespawnTask()
        viewFrame.with(TraderView())
    }

    private fun setupListeners(plugin: JavaPlugin, viewFrame: ViewFrame) = with(plugin.server.pluginManager) {
        registerEvents(TraderListener(viewFrame), plugin)
    }

}