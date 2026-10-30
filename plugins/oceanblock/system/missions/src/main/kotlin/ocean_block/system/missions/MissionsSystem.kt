package ocean_block.system.missions

import co.aikar.commands.PaperCommandManager
import ocean_block.system.missions.command.MissoesCommand
import ocean_block.system.missions.data.MissionManager
import ocean_block.system.missions.dialog.MissionDialog
import ocean_block.system.missions.listener.AccountResetListener
import ocean_block.system.missions.listener.MissionListener
import ocean_block.system.missions.notify.MissionNotifier
import one_block.textura.api.NotificationApi
import org.bukkit.plugin.java.JavaPlugin

object MissionsSystem {

    private const val DAY_CHECK_TICKS = 1200L

    fun init(plugin: JavaPlugin, paperCommandManager: PaperCommandManager) {
        MissionManager.onInit()
        NotificationApi.registerItemIcons(listOf(MissionNotifier.ICON_MATERIAL))
        MissionDialog.registerIcons()
        plugin.server.pluginManager.registerEvents(MissionListener(), plugin)
        plugin.server.pluginManager.registerEvents(AccountResetListener(), plugin)
        paperCommandManager.registerCommand(MissoesCommand())
        plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            plugin.server.onlinePlayers.forEach { MissionTracker.refresh(it) }
        }, DAY_CHECK_TICKS, DAY_CHECK_TICKS)
        plugin.server.onlinePlayers.forEach { MissionTracker.join(it) }
    }

    fun onDisable() {
        MissionManager.onDisable()
    }
}
