package ocean_block.system.playtime_rewards

import co.aikar.commands.PaperCommandManager
import ocean_block.system.playtime_rewards.command.RecompensasCommand
import ocean_block.system.playtime_rewards.config.RewardsFile
import ocean_block.system.playtime_rewards.data.PlaytimeRewardManager
import ocean_block.system.playtime_rewards.dialog.RewardDialog
import ocean_block.system.playtime_rewards.notify.RewardNotifier
import one_block.textura.api.NotificationApi
import org.bukkit.plugin.java.JavaPlugin

object PlaytimeRewardsSystem {

    fun init(plugin: JavaPlugin, paperCommandManager: PaperCommandManager) {
        PlaytimeRewards.ladder = RewardsFile.load(plugin)
        PlaytimeRewardManager.onInit()
        NotificationApi.registerItemIcons(listOf(RewardNotifier.ICON_MATERIAL))
        RewardDialog.registerIcons()
        val watcher = PlaytimeWatcher(plugin)
        plugin.server.pluginManager.registerEvents(watcher, plugin)
        plugin.server.pluginManager.registerEvents(RewardNotifier, plugin)
        watcher.start()
        paperCommandManager.registerCommand(RecompensasCommand())
    }

    fun onDisable() {
        PlaytimeRewardManager.onDisable()
    }
}
