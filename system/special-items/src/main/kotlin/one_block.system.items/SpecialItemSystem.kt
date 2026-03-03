package one_block.system.items

import one_block.system.islands.island.IslandManager
import one_block.system.islands.listener.IslandListener
import one_block.system.items.listener.BeaconListener
import one_block.system.items.sell.BeaconRepository
import one_block.system.items.sell.SellBeaconManager
import one_block.system.items.tasks.SellTask
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

object SpecialItemSystem {
    lateinit var plugin: JavaPlugin

    fun init(plugin: JavaPlugin) {
        this.plugin = plugin
        setupListeners(plugin)
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            SellBeaconManager.onInit() }, 20L)
    }

    private fun setupListeners(plugin: JavaPlugin) = with(plugin.server.pluginManager) {
        registerEvents(BeaconListener(plugin), plugin)
    }

}