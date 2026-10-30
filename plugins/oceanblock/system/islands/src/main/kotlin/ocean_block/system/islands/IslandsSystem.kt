package ocean_block.system.islands

import me.devnatan.inventoryframework.ViewFrame
import net.skinsrestorer.api.SkinsRestorerProvider
import net.skinsrestorer.api.event.SkinApplyEvent
import ocean_block.libs.instance.Instance
import ocean_block.system.islands.account.AccountReset
import ocean_block.system.islands.account.AccountResetListener
import ocean_block.system.islands.choice.ChoiceListener
import ocean_block.system.islands.expansion.IslandExpansion
import ocean_block.system.islands.generator.GeneratorBreakListener
import ocean_block.system.islands.generator.GeneratorFile
import ocean_block.system.islands.generator.GeneratorListener
import ocean_block.system.islands.island.IslandManager
import ocean_block.system.islands.island.type.IceGuardListener
import ocean_block.system.islands.member.InviteQuitListener
import ocean_block.system.islands.phase.PhaseManager
import ocean_block.system.islands.view.*
import one_block.textura.api.TitleApi
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

object IslandsSystem {

    lateinit var plugin: JavaPlugin

    fun init(plugin: JavaPlugin, viewFrame: ViewFrame) {
        this.plugin = plugin
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            IslandManager.onInit()
            AccountReset.load()
            SkinsRestorerProvider.get().eventBus.subscribe(Instance, SkinApplyEvent::class.java) { event ->
                val player = event.getPlayer(Player::class.java) ?: return@subscribe

                Bukkit.getScheduler().runTaskLater(Instance, Runnable {
                    val islandAt = IslandManager.getIslandAt(player.location) ?: return@Runnable
                    islandAt.onJoin(player)
                }, 3L)
            }
        }, 20L)
        PhaseManager.loadPhases()

        PhaseView.rowAscents().forEach { TitleApi.registerAscent(it) }

        viewFrame.with(
            PhaseView(),
            PhaseDetailView(),
            ChestLootView(),
            IslandsView(),
            IslandView(),
            MembersView(),
            UpgradeView(),
            SettingsView(),
            VisitsView(),
            PlayerPickerView()
        )

        plugin.server.pluginManager.registerEvents(GeneratorListener(GeneratorFile.load(plugin)), plugin)
        plugin.server.pluginManager.registerEvents(GeneratorBreakListener(), plugin)
        plugin.server.pluginManager.registerEvents(InviteQuitListener(), plugin)
        plugin.server.pluginManager.registerEvents(IceGuardListener(), plugin)
        plugin.server.pluginManager.registerEvents(ChoiceListener(), plugin)
        plugin.server.pluginManager.registerEvents(AccountResetListener(), plugin)

        IslandExpansion().register()
    }

    fun onDisable() {
        IslandManager.onDisable()
    }
}
