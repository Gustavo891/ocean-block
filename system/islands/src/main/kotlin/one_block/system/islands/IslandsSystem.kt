package one_block.system.islands

import me.devnatan.inventoryframework.ViewFrame
import net.skinsrestorer.api.SkinsRestorerProvider
import net.skinsrestorer.api.event.SkinApplyEvent
import one_block.libs.instance.Instance
import one_block.system.islands.expansion.IslandExpansion
import one_block.system.islands.island.IslandManager
import one_block.system.islands.phase.PhaseManager
import one_block.system.islands.view.*
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

object IslandsSystem {

    lateinit var plugin: JavaPlugin

    fun init(plugin: JavaPlugin, viewFrame: ViewFrame) {
        this.plugin = plugin
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            IslandManager.onInit()
            SkinsRestorerProvider.get().eventBus.subscribe(Instance, SkinApplyEvent::class.java) { event ->
                val player = event.getPlayer(Player::class.java) ?: return@subscribe

                Bukkit.getScheduler().runTaskLater(Instance, Runnable {
                    val islandAt = IslandManager.getIslandAt(player.location) ?: return@Runnable
                    islandAt.onJoin(player)
                }, 3L)
            }
        }, 20L)
        PhaseManager.loadPhases()
        viewFrame.with(
            PhaseView(),
            IslandsView(),
            IslandView(),
            MembersView(),
            UpgradeView()
        )

        IslandExpansion().register()
    }
}