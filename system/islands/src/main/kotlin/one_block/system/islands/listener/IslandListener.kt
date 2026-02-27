package one_block.system.islands.listener

import one_block.system.islands.island.IslandManager
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.TNTPrimed
import org.bukkit.entity.Tameable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityExplodeEvent

class IslandListener : Listener {

    @EventHandler
    fun onEntityExplode(event: EntityExplodeEvent) {
        val island = IslandManager.getIslandAt(event.location) ?: return
        event.blockList().clear()
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val victim = event.entity
        if (victim !is Player) return

        val attacker = getPlayerAttacker(event) ?: return
        if (attacker == victim) return

        val islandAt = IslandManager.getIslandAt(victim.location)
        if (islandAt != null) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val island = IslandManager.getIslandByPlayer(player.uniqueId) ?: return

        if (island.ownerUuid == player.uniqueId) return
        if (island.members[player.name.lowercase()] != null) return
        if (player.name.lowercase() in island.trustedNames) return

        event.isCancelled = true
    }

    private fun getPlayerAttacker(event: EntityDamageByEntityEvent): Player? {
        return when (val damager = event.damager) {
            is Player -> damager
            is Projectile -> damager.shooter as? Player
            is TNTPrimed -> damager.source as? Player
            is Tameable -> if (damager.isTamed) damager.owner as? Player else null
            else -> null
        }
    }
}