package one_block.system.sell

import me.devnatan.inventoryframework.ViewFrame
import one_block.system.sell.manager.SellManager
import one_block.system.sell.view.SellView
import org.bukkit.plugin.java.JavaPlugin

object SellSystem {

    fun init(plugin: JavaPlugin, viewFrame: ViewFrame) {
        SellManager.load()
        viewFrame.with(SellView())

    }
}