package one_block.trader.listener

import me.devnatan.inventoryframework.ViewFrame
import one_block.system.islands.island.IslandManager
import one_block.trader.TraderSystem
import one_block.trader.manager.TraderManager
import one_block.trader.view.TraderView
import org.bukkit.Bukkit
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.WanderingTrader
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.server.PluginDisableEvent

class TraderListener(private val viewFrame: ViewFrame): Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    fun onTraderInteract(event: PlayerInteractEntityEvent) {

        val entity = event.rightClicked
        if (entity.type != EntityType.WANDERING_TRADER) return
        val trader = entity as WanderingTrader

        val activeTrader= TraderManager.activeTraders[trader.uniqueId] ?: return
        event.isCancelled = true

        val player = event.player
        viewFrame.open(TraderView::class.java, player, activeTrader)

    }

    @EventHandler
    fun onServerStop(event: PluginDisableEvent) {
        if (event.plugin != TraderSystem.plugin) return

        TraderManager.activeTraders.toMap().forEach { (traderEntityId, activeTrader) ->
            Bukkit.getEntity(traderEntityId)?.remove()

            val island = IslandManager.islandsById[activeTrader.islandId] ?: return@forEach

            TraderManager.traderRemove(traderEntityId, island)
        }
    }

}