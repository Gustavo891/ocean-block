package one_block.system.combatlog

import me.devnatan.inventoryframework.ViewFrame
import one_block.system.combatlog.listener.PlayerListener
import org.bukkit.plugin.java.JavaPlugin

object CombatLogSystem {

    fun init(plugin: JavaPlugin, viewFrame: ViewFrame) {
        plugin.server.pluginManager.registerEvents(PlayerListener(), plugin)
    }
}
