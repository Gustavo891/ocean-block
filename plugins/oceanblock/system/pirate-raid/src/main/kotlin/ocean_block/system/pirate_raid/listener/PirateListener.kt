package ocean_block.system.pirate_raid.listener

import ocean_block.system.pirate_raid.crew.PirateFactory
import ocean_block.system.pirate_raid.crew.PirateKeys
import ocean_block.system.pirate_raid.loot.RaidLoot
import ocean_block.system.pirate_raid.raid.RaidDirector
import org.bukkit.entity.Entity
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityBreakDoorEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import kotlin.random.Random

class PirateListener : Listener {

    @EventHandler(priority = EventPriority.HIGH)
    fun onDeath(event: EntityDeathEvent) {
        val pirate = event.entity
        val island = PirateKeys.islandOf(pirate) ?: return
        val kind = PirateKeys.kindOf(pirate) ?: return
        event.drops.clear()
        PirateFactory.forget(pirate)
        val raid = RaidDirector.raidOf(island)
        RaidLoot.drop(pirate.location, kind, island, raid?.level ?: RaidDirector.config.minLevel, RaidDirector.config, Random.Default)
        RaidDirector.pirateDied(island, pirate.uniqueId)
    }

    @EventHandler(ignoreCancelled = true)
    fun onChangeBlock(event: EntityChangeBlockEvent) {
        if (PirateKeys.isPirate(event.entity)) event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onBreakDoor(event: EntityBreakDoorEvent) {
        if (PirateKeys.isPirate(event.entity)) event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onFriendlyFire(event: EntityDamageByEntityEvent) {
        if (!PirateKeys.isPirate(event.entity)) return
        if (shooter(event.damager)?.let(PirateKeys::isPirate) == true) event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onTarget(event: EntityTargetLivingEntityEvent) {
        val target = event.target ?: return
        if (PirateKeys.isPirate(event.entity) && PirateKeys.isPirate(target)) event.isCancelled = true
    }

    private fun shooter(damager: Entity): Entity? =
        if (damager is Projectile) damager.shooter as? Entity else damager
}
