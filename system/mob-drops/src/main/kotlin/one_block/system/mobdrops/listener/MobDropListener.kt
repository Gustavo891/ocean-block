package one_block.system.mobdrops.listener

import one_block.system.mobdrops.manager.MobDropManager
import one_block.system.mobdrops.MobDrops
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent

class MobDropListener : Listener {

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity
        val drops = event.drops

        val removed = MobDropManager.removedDrops[entity.type]
        if (removed != null) {
            drops.removeIf { removed.contains(it.type) }
        }

        val customDrops = MobDrops.getDrops(entity.type)

        for (drop in customDrops) {
            if (Math.random() <= drop.chance) {
                drops.add(drop.item())
            }
        }
    }
}