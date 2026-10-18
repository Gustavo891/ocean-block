package io.github.badnotice.one_block.listener

import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent

class MobControlListener : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        if (event.entityType == EntityType.PHANTOM) {
            event.isCancelled = true
        }
    }
}
