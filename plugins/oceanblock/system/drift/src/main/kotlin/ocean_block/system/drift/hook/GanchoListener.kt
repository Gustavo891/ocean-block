package ocean_block.system.drift.hook

import ocean_block.system.custom_items.item.CustomItemRegistry
import ocean_block.system.drift.DriftSystem
import ocean_block.system.drift.collect.CollectorRule
import ocean_block.system.drift.debris.DebrisManager
import ocean_block.system.islands.island.IslandManager
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.inventory.EquipmentSlot

class GanchoListener : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onFish(event: PlayerFishEvent) {
        if (event.state !in PULL_STATES) return

        val config = DriftSystem.config ?: return
        val player = event.player
        if (!castWithGancho(event, player)) return

        val location = event.hook.location
        val island = IslandManager.getIslandAt(location) ?: return
        if (!CollectorRule.canCollect(island, player)) return

        DebrisManager.nearestFree(location, config.settings.hookRadius)
            ?.let { DebrisManager.beginPull(it, player) }
    }

    private fun castWithGancho(event: PlayerFishEvent, player: Player): Boolean {
        val inventory = player.inventory
        val stack = when (event.hand) {
            EquipmentSlot.HAND -> inventory.itemInMainHand
            EquipmentSlot.OFF_HAND -> inventory.itemInOffHand
            else -> return holdsGanchoByPriority(player)
        }
        return CustomItemRegistry.fromItemStack(stack) === GanchoItem
    }

    private fun holdsGanchoByPriority(player: Player): Boolean {
        val inventory = player.inventory
        val main = inventory.itemInMainHand
        if (CustomItemRegistry.fromItemStack(main) === GanchoItem) return true
        if (main.type == Material.FISHING_ROD) return false
        return CustomItemRegistry.fromItemStack(inventory.itemInOffHand) === GanchoItem
    }

    private companion object {
        val PULL_STATES = setOf(
            PlayerFishEvent.State.REEL_IN,
            PlayerFishEvent.State.FAILED_ATTEMPT,
            PlayerFishEvent.State.IN_GROUND,
        )
    }
}
