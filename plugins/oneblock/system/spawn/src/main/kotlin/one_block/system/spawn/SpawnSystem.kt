package one_block.system.spawn

import co.aikar.commands.PaperCommandManager
import one_block.system.spawn.command.BuildCommand
import one_block.system.spawn.command.NpcCommand
import one_block.system.spawn.command.SetSpawnCommand
import one_block.system.spawn.command.SpawnCommand
import one_block.system.spawn.hologram.SpawnHolograms
import one_block.system.spawn.listener.NpcListener
import one_block.system.spawn.listener.NpcModelListener
import one_block.system.spawn.listener.PortalListener
import one_block.system.spawn.listener.SpawnHudListener
import one_block.system.spawn.listener.SpawnProtectionListener
import one_block.system.spawn.npc.NpcManager
import one_block.system.spawn.npc.NpcModel
import one_block.system.spawn.portal.IslandPortal
import one_block.system.spawn.spawn.SpawnManager
import one_block.system.spawn.world.SpawnWorldRules
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
        registerEvents(SpawnHudListener(), plugin)
        registerEvents(SpawnProtectionListener(), plugin)
        registerEvents(NpcListener(), plugin)
        registerEvents(PortalListener(), plugin)

        if (NpcModel.isAvailable()) registerEvents(NpcModelListener(), plugin)
    }
}
