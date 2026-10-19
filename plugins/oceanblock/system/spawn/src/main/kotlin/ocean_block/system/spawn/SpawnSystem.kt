package ocean_block.system.spawn

import co.aikar.commands.PaperCommandManager
import ocean_block.system.spawn.command.BuildCommand
import ocean_block.system.spawn.command.NpcCommand
import ocean_block.system.spawn.command.SetSpawnCommand
import ocean_block.system.spawn.command.SpawnCommand
import ocean_block.system.spawn.hologram.SpawnHolograms
import ocean_block.system.spawn.listener.NpcListener
import ocean_block.system.spawn.listener.NpcModelListener
import ocean_block.system.spawn.listener.PortalListener
import ocean_block.system.spawn.listener.SpawnProtectionListener
import ocean_block.system.spawn.npc.NpcManager
import ocean_block.system.spawn.npc.NpcModel
import ocean_block.system.spawn.portal.IslandPortal
import ocean_block.system.spawn.spawn.SpawnManager
import ocean_block.system.spawn.world.SpawnWorldRules
import org.bukkit.plugin.java.JavaPlugin

object SpawnSystem {

    private const val SPAWN_DELAY_TICKS = 40L

    fun init(plugin: JavaPlugin, commandManager: PaperCommandManager) {
        SpawnManager.onInit()
        NpcManager.onInit()

        setupCommands(commandManager)
        setupListeners(plugin)

        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            SpawnWorldRules.apply()
            SpawnHolograms.spawnAll()
            IslandPortal.start()
            NpcManager.spawnAll()
        }, SPAWN_DELAY_TICKS)
    }

    fun onDisable() {
        IslandPortal.stop()
        SpawnHolograms.despawnAll()
        NpcManager.despawnAll()
    }

    private fun setupCommands(commandManager: PaperCommandManager) = with(commandManager) {
        commandCompletions.registerCompletion("spawn-npcs") {
            NpcManager.all().map { npc -> npc.id }
        }

        registerCommand(SpawnCommand())
        registerCommand(SetSpawnCommand())
        registerCommand(BuildCommand())
        registerCommand(NpcCommand())
    }

    private fun setupListeners(plugin: JavaPlugin) = with(plugin.server.pluginManager) {
        registerEvents(SpawnProtectionListener(), plugin)
        registerEvents(NpcListener(), plugin)
        registerEvents(PortalListener(), plugin)

        if (NpcModel.isAvailable()) registerEvents(NpcModelListener(), plugin)
    }
}
