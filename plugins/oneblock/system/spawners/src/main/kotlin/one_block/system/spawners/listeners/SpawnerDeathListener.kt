package one_block.system.spawners.listeners

import one_block.system.spawners.manager.SpawnerEntity
import one_block.system.spawners.manager.SpawnerKill
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityTargetEvent

class SpawnerDeathListener : Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    fun onDamage(event: EntityDamageEvent) {
        val entity = event.entity as? LivingEntity ?: return
        val ownerUuid = SpawnerEntity.ownerOf(entity) ?: return

        val killer = (event as? EntityDamageByEntityEvent)?.damager as? Player
        if (killer == null) {
            event.isCancelled = true
            return
        }

        if (!SpawnerKill.isLethal(entity, event.finalDamage)) return

        val dies = SpawnerKill.apply(entity, ownerUuid, killer, sweep = killer.isSneaking)
        if (!dies) event.damage = 0.0
    }

    @EventHandler(ignoreCancelled = true)
    fun onTarget(event: EntityTargetEvent) {
        val entity = event.entity as? LivingEntity ?: return
        if (SpawnerEntity.ownerOf(entity) != null) event.isCancelled = true
    }

    @EventHandler
    fun onDeath(event: EntityDeathEvent) {
        if (SpawnerEntity.ownerOf(event.entity) == null) return

        event.drops.clear()
        event.droppedExp = 0
    }
}
