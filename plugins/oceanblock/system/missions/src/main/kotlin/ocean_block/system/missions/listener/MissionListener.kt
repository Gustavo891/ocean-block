package ocean_block.system.missions.listener

import ocean_block.libs.instance.Instance
import ocean_block.system.craft.events.CustomCraftEvent
import ocean_block.system.drift.collect.DebrisCollectedEvent
import ocean_block.system.islands.generator.GeneratorBlockBrokenEvent
import ocean_block.system.islands.generator.GeneratorFormedEvent
import ocean_block.system.islands.island.IslandUpgradeEvent
import ocean_block.system.level.PlayerLevelUpEvent
import ocean_block.system.missions.MissionTracker
import ocean_block.system.missions.hud.MissionHud
import ocean_block.system.missions.model.MissionSignal
import ocean_block.system.sell.listener.SellEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import kotlin.math.roundToLong

class MissionListener : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onDebris(event: DebrisCollectedEvent) {
        MissionTracker.signal(event.player, MissionSignal.Debris(event.byHook))
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onGeneratorBlock(event: GeneratorBlockBrokenEvent) {
        MissionTracker.signal(event.player, MissionSignal.GeneratorBlock(event.material.name))
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onGeneratorFormed(event: GeneratorFormedEvent) {
        event.island.getIslandOnlinePlayers().forEach { MissionTracker.signal(it, MissionSignal.GeneratorFormed) }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onCraft(event: CustomCraftEvent) {
        MissionTracker.signal(event.player, MissionSignal.Crafted(event.id))
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onSell(event: SellEvent) {
        MissionTracker.signal(event.player, MissionSignal.Sold(event.amount.roundToLong()))
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onLevelUp(event: PlayerLevelUpEvent) {
        MissionTracker.signal(event.player, MissionTracker.stateOf(event.player))
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onUpgrade(event: IslandUpgradeEvent) {
        event.island.getIslandOnlinePlayers().forEach { MissionTracker.signal(it, MissionTracker.stateOf(it)) }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        Instance.server.scheduler.runTaskLater(Instance, Runnable {
            if (player.isOnline) MissionTracker.join(player)
        }, 1L)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        MissionHud.hide(event.player)
    }
}
