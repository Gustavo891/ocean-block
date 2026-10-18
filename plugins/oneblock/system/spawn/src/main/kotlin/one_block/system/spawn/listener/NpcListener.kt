package one_block.system.spawn.listener

import one_block.system.spawn.npc.NpcManager
import one_block.system.spawn.npc.packet.PacketNpcService
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot

class NpcListener : Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEntityEvent) {
        if (event.hand != EquipmentSlot.HAND) return

        val id = NpcManager.npcIdOf(event.rightClicked) ?: return

        event.isCancelled = true
        NpcManager.interact(event.player, id)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) = PacketNpcService.forget(event.player.uniqueId)
}
