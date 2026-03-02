package one_block.system.islands.listener

import one_block.system.islands.island.Island
import one_block.system.islands.island.IslandManager
import one_block.system.islands.island.IslandMemberPermission
import org.bukkit.Bukkit
import org.bukkit.Particle
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

        if(!isTrustAllowed(island, player)) {
            player.sendRichMessage("<red>Você só pode colocar blocos enquanto o dono da ilha estiver online.")
            return
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val island = IslandManager.getIslandAt(player.location) ?: return

        if(!isTrustAllowed(island, player)) {
            player.sendRichMessage("<red>Você só pode quebrar blocos enquanto o dono da ilha estiver online.")
            return
        }
    }

    private fun isTrustAllowed(island: Island, player: Player) : Boolean {
        if (island.ownerUuid == player.uniqueId) return true

        val islandMember = island.members[player.name.lowercase()]
        if (islandMember != null) return true

        if(island.isTrustedMember(player.name)) {
            val owner = Bukkit.getPlayer(island.ownerUuid);
            if(owner == null || !owner.isOnline) {
                return false
            }
        }
        return true
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

    @EventHandler
    fun cropGrowth(event: BlockGrowEvent) {
        val data = event.newState.blockData
        if (data !is Ageable) return

        val island = IslandManager.getIslandAt(event.block.location) ?: return
        val level = island.upgrades["crop"] ?: 0

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

}