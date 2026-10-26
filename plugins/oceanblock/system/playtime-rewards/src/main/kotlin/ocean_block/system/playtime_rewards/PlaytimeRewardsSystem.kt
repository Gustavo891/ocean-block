package ocean_block.system.playtime_rewards

import co.aikar.commands.PaperCommandManager
import ocean_block.system.playtime_rewards.command.RecompensasCommand
import ocean_block.system.playtime_rewards.config.RewardsFile
import ocean_block.system.playtime_rewards.data.PlaytimeRewardManager
import ocean_block.system.playtime_rewards.dialog.RewardDialog
import ocean_block.system.playtime_rewards.model.Milestone
import ocean_block.system.playtime_rewards.notify.RewardNotifier
import one_block.textura.api.NotificationApi
import org.bukkit.plugin.java.JavaPlugin

object PlaytimeRewardsSystem {

    private const val SAMPLE_PENDING = 2
    private val SAMPLE_MILESTONE = Milestone("exemplo", 60, 0, 0, 0)

    fun init(plugin: JavaPlugin, paperCommandManager: PaperCommandManager) {
        PlaytimeRewards.ladder = RewardsFile.load(plugin)
        PlaytimeRewardManager.onInit()
        NotificationApi.registerItemIcons(listOf(RewardNotifier.ICON_MATERIAL))
        NotificationApi.registerSample("tempo") { RewardNotifier.reached(it, PlaytimeRewards.ladder.firstOrNull() ?: SAMPLE_MILESTONE) }
        NotificationApi.registerSample("recompensas_pendentes") { RewardNotifier.pending(it, SAMPLE_PENDING) }
        RewardDialog.registerIcons()
        val watcher = PlaytimeWatcher(plugin)
        plugin.server.pluginManager.registerEvents(watcher, plugin)
        watcher.start()
        paperCommandManager.registerCommand(RecompensasCommand())
    }

    fun onDisable() {
        PlaytimeRewardManager.onDisable()
    }
}
