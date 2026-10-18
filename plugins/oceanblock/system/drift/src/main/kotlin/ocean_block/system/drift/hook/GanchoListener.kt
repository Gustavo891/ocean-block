package ocean_block.system.drift.hook

import ocean_block.system.custom_items.item.CustomItemRegistry
import ocean_block.system.drift.DriftSystem
import ocean_block.system.drift.collect.CollectorRule
import ocean_block.system.drift.debris.DebrisManager
import ocean_block.system.islands.island.IslandManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerFishEvent

class GanchoListener : Listener {

    @EventHandler
    fun onFish(event: PlayerFishEvent) {
        if (event.state !in PULL_STATES) return

        val config = DriftSystem.config ?: return
        val player = event.player
        if (!holdsGancho(player)) return

        val location = event.hook.location
        val island = IslandManager.getIslandAt(location) ?: return
        if (!CollectorRule.canCollect(island, player)) return

        DebrisManager.nearestFree(location, config.settings.hookRadius)
            ?.let { DebrisManager.beginPull(it, player) }
    }

    private fun holdsGancho(player: Player): Boolean =
        CustomItemRegistry.fromItemStack(player.inventory.itemInMainHand) === GanchoItem ||
            CustomItemRegistry.fromItemStack(player.inventory.itemInOffHand) === GanchoItem

    private companion object {
        val PULL_STATES = setOf(
            PlayerFishEvent.State.REEL_IN,
            PlayerFishEvent.State.FAILED_ATTEMPT,
            PlayerFishEvent.State.IN_GROUND,
        )
    }
}
