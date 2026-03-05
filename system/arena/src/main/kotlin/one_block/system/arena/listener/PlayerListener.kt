package one_block.system.arena.listener

import net.kyori.adventure.text.minimessage.MiniMessage
import one_block.system.arena.ArenaSystem
import one_block.system.islands.island.IslandManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.entity.WindCharge
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerRespawnEvent

class PlayerListener : Listener {

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        if (player.world.name != "spawn") return

        val location = event.block.location
        if (player.hasPermission("arena.bypass")) return

        if (ArenaSystem.arena.contains(
                location.blockX,
                location.blockY,
                location.blockZ
            )
        ) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockPlaceEvent) {
        val player = event.player
        if (player.world.name != "spawn") return

        val location = event.block.location
        if (player.hasPermission("arena.bypass")) return

        if (ArenaSystem.arena.contains(
                location.blockX,
                location.blockY,
                location.blockZ
            )
        ) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerDamage(event: EntityDamageByEntityEvent) {
        val victim = event.entity
        if (victim.world.name != "spawn") return

        val damager = event.damager

        if (victim !is Player) return

        val playerDamager = when {
            damager is Player -> damager
            damager is org.bukkit.entity.Projectile && damager.shooter is Player -> damager.shooter as Player
            else -> null
        }

        val safeZone = ArenaSystem.safeZone
        val victimInside = safeZone.contains(
            victim.location.blockX,
            victim.location.blockY,
            victim.location.blockZ
        )

        val damagerInside = playerDamager?.let {
            safeZone.contains(
                it.location.blockX,
                it.location.blockY,
                it.location.blockZ
            )
        } ?: false

        if (victimInside || damagerInside) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onProjectileLaunch(event: ProjectileLaunchEvent) {
        val projectile = event.entity
        if (projectile.world.name != "spawn") return

        val isProtected = isProtected(projectile.location)
        if (!isProtected) return

        if (projectile is WindCharge) {
            projectile.acceleration = projectile.acceleration.multiply(0)
        }
    }

    @EventHandler
    fun onFish(event: PlayerFishEvent) {
        if (event.state != PlayerFishEvent.State.CAUGHT_ENTITY) return

        val caught = event.caught ?: return
        if (caught.world.name != "spawn") return
        val location = caught.location

        val isProtected = isProtected(location)
        if (!isProtected) return

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onWindChargeHit(event: EntityExplodeEvent) {
        val entity = event.entity
        if (entity.world.name != "spawn") return

        if (entity is WindCharge) {
            val location = entity.location

            val isProtected = isProtected(location)
            if (isProtected) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player

        val island = IslandManager.islandsById.values.find { it.ownerUuid == player.uniqueId }
        if (island == null) return

        event.respawnLocation = island.spawn.add(.0, 2.0, .0).toBukkit()!!
    }

    fun isProtected(location: Location): Boolean {
        return ArenaSystem.safeZone.contains(
            location.blockX,
            location.blockY,
            location.blockZ
        )
    }
}