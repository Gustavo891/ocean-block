package ocean_block.system.sell

import me.devnatan.inventoryframework.ViewFrame
import ocean_block.system.sell.manager.SellManager
import ocean_block.system.sell.view.SellItemsView
import ocean_block.system.sell.view.SellView
import org.bukkit.plugin.java.JavaPlugin

object SellSystem {

    lateinit var plugin: JavaPlugin

    fun init(plugin: JavaPlugin, viewFrame: ViewFrame) {
        this.plugin = plugin
        SellManager.load()
        viewFrame.with(SellView())
        viewFrame.with(SellItemsView())
    }
}
