package one_block.system.combatlog.listener

import one_block.system.combatlog.combat_player.CombatPlayerManager
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.TNTPrimed
import org.bukkit.entity.Tameable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerToggleFlightEvent

class PlayerListener(
) : Listener {

    private val Player.isInCombat
        get() = CombatPlayerManager.isPlayerInCombat(this) && !hasPermission("screen.mod")

    @EventHandler(ignoreCancelled = true)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val victim = event.entity
        if (victim !is Player) return
        if (victim.world.name == "world") return

        val attacker = getPlayerAttacker(event) ?: return
        if (attacker == victim) return

        CombatPlayerManager.insert(attacker, victim)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity

        val inCombat = CombatPlayerManager.removeCombat(player) ?: return
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun a(event: EntityDamageByEntityEvent) {
        val entity = event.entity as? Player ?: return

        val finalDamage = event.finalDamage

        if (entity.health - finalDamage <= 0) {
            val inCombat = CombatPlayerManager.removeCombat(entity) ?: return
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player

        val inCombat = CombatPlayerManager.removeCombat(player) ?: return
    }


    @EventHandler(priority = EventPriority.LOWEST)
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        if (!player.isInCombat) return

        val loc = player.location
        player.inventory.contents.filterNotNull().forEach {
            loc.world?.dropItemNaturally(loc, it)
        }

        player.inventory.clear()


        player.health = 0.0

        CombatPlayerManager.removeCombat(player)
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerCommandPreprocess(event: PlayerCommandPreprocessEvent) {
        val player = event.player
        if (!player.isInCombat) return

        event.isCancelled = true
        player.sendRichMessage("<red>Você não pode utilizar comandos em combate!")
    }

    @EventHandler
    fun onPlayerToggleFlight(event: PlayerToggleFlightEvent) {
        val player = event.player
        if (!player.isInCombat) return

        if (event.isFlying) {
            event.isCancelled = true
            player.sendRichMessage("<red>Você não pode voar em combate!")
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
