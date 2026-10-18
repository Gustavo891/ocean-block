package ocean_block.system.islands.listener

import ocean_block.system.islands.island.IslandManager
import ocean_block.system.islands.island.IslandUpgrade
import org.bukkit.block.data.Ageable
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.TNTPrimed
import org.bukkit.entity.Tameable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockGrowEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerInteractEvent

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
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        val island = IslandManager.getIslandAt(player.location) ?: return


        if(!island.isTrustAllowed(player)) {
            event.isCancelled = true
            return
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        val island = IslandManager.getIslandAt(player.location) ?: return

        if(!island.isTrustAllowed(player)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val island = IslandManager.getIslandAt(player.location) ?: return

        if(!island.isTrustAllowed(player)) {
            event.isCancelled = true
            return
        }
    }

    @EventHandler
    fun onBlockGrowth(event: BlockGrowEvent) {
        val data = event.newState.blockData
        if (data !is Ageable) return

        val island = IslandManager.getIslandAt(event.block.location) ?: return
        val level = island.getUpgradeLevel(IslandUpgrade.CropGrowth)

        val cancelChance = when (level) {
            0 -> 0.50
            1 -> 0.40
            2 -> 0.25
            3 -> 0.10
            4 -> 0.0
            5 -> -1.0
            else -> 0.50
        }

        if (cancelChance > 0 && Math.random() < cancelChance) {
            event.isCancelled = true
        }
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
