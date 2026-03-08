package one_block.system.combatlog.listener

import one_block.system.combatlog.combat_player.CombatPlayerManager
import org.bukkit.Bukkit
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

/*
    @EventHandler
    fun onPreTeleport(event: AsyncPrePlayerTeleportEvent) {
        try {
            val playerName = event.playerName
            plugin.logger.warning("[AsyncPrePlayerTeleportEvent] Player $playerName called")

            val player = Bukkit.getPlayerExact(playerName)
            if (player == null) {
                plugin.logger.warning("[AsyncPrePlayerTeleportEvent] Player $playerName not found")
                event.isCancelled = true
                return
            }

            val inCombat = player.isInCombat
            plugin.logger.info("[AsyncPrePlayerTeleportEvent] ${player.name} (combat: ${player.isInCombat})")
            if (!inCombat) return

            event.isCancelled = true
        } catch (e: Exception) {
            plugin.logger.severe("Error processing AsyncPrePlayerTeleportEvent: ${e.message}")
            e.printStackTrace()
        } catch (e: Error) {
            plugin.logger.severe("Error processing AsyncPrePlayerTeleportEvent: ${e.message}")
            e.printStackTrace()
        } catch (t: Throwable) {
            plugin.logger.severe("Error processing AsyncPrePlayerTeleportEvent: ${t.message}")
            t.printStackTrace()
        }
    }
*/

    @EventHandler(priority = EventPriority.LOWEST)
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        if (!player.isInCombat) return

        val loc = player.location
        player.inventory.contents.filterNotNull().forEach {
            loc.world?.dropItemNaturally(loc, it)
        }

        player.inventory.clear()

    /*    MongoPlayerSyncRepository.update(
            PlayerSync.from(player)
        )*/

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

    /*    @EventHandler
        https://github.com/PaperMC/Paper/issues/12696
        fun onPlayerTeleport(event: PlayerTeleportEvent) {
            val player = event.player
            if (!player.isInCombat) return

            event.from = player.location
            event.to = player.location

            if (event.cause == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
                player.message("${Colors.RED}Você não pode usar enderpearl em combate!")
            }
        }*/

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