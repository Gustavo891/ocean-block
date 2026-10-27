package ocean_block.system.pirate_raid

import co.aikar.commands.PaperCommandManager
import ocean_block.system.pirate_raid.command.PiratasCommand
import ocean_block.system.pirate_raid.config.RaidFile
import ocean_block.system.pirate_raid.crew.PirateFactory
import ocean_block.system.pirate_raid.crew.PirateKeys
import ocean_block.system.pirate_raid.data.RaidDays
import ocean_block.system.pirate_raid.listener.LootPickupListener
import ocean_block.system.pirate_raid.listener.PirateListener
import ocean_block.system.pirate_raid.notify.RaidNotifier
import ocean_block.system.pirate_raid.raid.RaidDirector
import one_block.textura.api.NotificationApi
import org.bukkit.Bukkit
import org.bukkit.entity.Item
import org.bukkit.plugin.java.JavaPlugin

object PirateRaidSystem {

    fun init(plugin: JavaPlugin, paperCommandManager: PaperCommandManager) {
        RaidDays.onInit()
        PirateFactory.setupTeams()
        RaidDirector.start(plugin, RaidFile.load(plugin))
        plugin.server.pluginManager.registerEvents(PirateListener(), plugin)
        plugin.server.pluginManager.registerEvents(LootPickupListener(), plugin)
        paperCommandManager.registerCommand(PiratasCommand())
        NotificationApi.registerSample("piratas") { RaidNotifier.started(it) }
    }

    fun onDisable() {
        RaidDirector.shutdown()
        Bukkit.getWorlds().flatMap { it.entities }
            .filter { PirateKeys.isPirate(it) || PirateKeys.isRaidBoat(it) || (it is Item && PirateKeys.isRaidLoot(it.itemStack)) }
            .forEach { it.remove() }
    }
}
