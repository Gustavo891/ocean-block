package one_block.system.sell

import one_block.system.sell.manager.SellManager
import org.bukkit.plugin.java.JavaPlugin

object SellSystem {

    fun init(plugin: JavaPlugin) {
        SellManager.load()
    }
}