package ocean_block.system.collections.listener

import ocean_block.system.collections.Collections
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent

class CollectionListener : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPickup(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        val stack = event.item.itemStack

        Collections.add(player.uniqueId, stack.type, stack.amount.toLong())
    }
}
