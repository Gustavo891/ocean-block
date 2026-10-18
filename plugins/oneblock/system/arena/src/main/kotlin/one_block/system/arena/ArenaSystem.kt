package one_block.system.arena

import co.aikar.commands.PaperCommandManager
import me.devnatan.inventoryframework.ViewFrame
import one_block.libs.coordinate.SimpleLocation
import one_block.system.arena.command.ArenaCommand
import one_block.system.arena.listener.PlayerListener
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector

object ArenaSystem {

    val spawnPosition by lazy { SimpleLocation(1019.5, 103.0, 985.5, world = "spawn") }

    val safeZone by lazy {
        Cuboid.from(
            Vector(1014.5, 102.0, 990.5),
            Vector(1024.5, 110.0, 980.5)
        )
    }

    val arena by lazy {
        Cuboid.from(
            Vector(913.5, 90.0, 907.5),
            Vector(1077.5, 163.0, 1080.5)
        )
    }

    fun init(plugin: JavaPlugin, viewFrame: ViewFrame) {
        setupListeners(plugin)
    }

    fun setupCommands(commandManager: PaperCommandManager) {
        commandManager.registerCommand(ArenaCommand())
    }

    private fun setupListeners(plugin: JavaPlugin) = with(plugin.server.pluginManager) {
        registerEvents(PlayerListener(), plugin)
    }
}
