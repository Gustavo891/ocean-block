package one_block.system.minions.listener

import one_block.system.minions.placed_minion.PlacedMinionManager
import org.bukkit.entity.ArmorStand
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerArmorStandManipulateEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent

class PlayerListener : Listener {

    @EventHandler
    fun onPlayerArmorStandManipulate(event: PlayerArmorStandManipulateEvent) {
        val rightClicked = event.rightClicked
        val placedMinion = PlacedMinionManager.get(rightClicked.location) ?: return

        event.isCancelled = true
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val entity = event.entity

        if (entity !is ArmorStand) return
        val placedMinion = PlacedMinionManager.get(entity.location) ?: return

        event.isCancelled = true
    }

    @EventHandler
    fun onInteractAtEntity(event: PlayerInteractAtEntityEvent) {
        val entity = event.rightClicked
        if (entity !is ArmorStand) return

        val placedMinion = PlacedMinionManager.get(entity.location) ?: return

        val player = event.player
        val item = player.inventory.itemInMainHand

        event.isCancelled = true
        player.sendMessage("Você adicionou energia ao minion!")
    }
}