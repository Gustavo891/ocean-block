package one_block.system.arena.listener

import one_block.system.arena.ArenaSystem
import one_block.system.islands.island.IslandManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.WindCharge
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerBucketFillEvent
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

        val island = IslandManager.getIslandByOwnerUUID(player.uniqueId)
        if (island == null) return

        event.respawnLocation = island.spawn.add(.0, 2.0, .0).toBukkit() ?: return
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        dispatchDeathMessage(event)
    }

    @EventHandler
    fun onBucketEmpty(event: PlayerBucketEmptyEvent) {
        if (event.player.world.name != "spawn") return
        event.isCancelled = true
    }

    @EventHandler
    fun onBucketFill(event: PlayerBucketFillEvent) {
        if (event.player.world.name != "spawn") return
        event.isCancelled = true
    }

    fun isProtected(location: Location): Boolean {
        return ArenaSystem.safeZone.contains(
            location.blockX,
            location.blockY,
            location.blockZ
        )
    }

    private fun dispatchDeathMessage(event: PlayerDeathEvent) {
        event.showDeathMessages = false

        val player = event.entity
        val cause = player.lastDamageCause?.cause ?: return
        val damager = (player.lastDamageCause as? EntityDamageByEntityEvent)?.damager

        val prettyName = player.name

        val message = when (cause) {
            EntityDamageEvent.DamageCause.ENTITY_ATTACK,
            EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK ->
                when (damager) {
                    is Player -> "$prettyName <dark_red>foi morto em combate por ${damager.name}<dark_red>!"
                    is LivingEntity -> "$prettyName <dark_red>foi morto por um monstro!"
                    else -> "$prettyName <dark_red>foi morto em combate!"
                }

            EntityDamageEvent.DamageCause.PROJECTILE ->
                if (damager is Projectile && damager.shooter is Player) {
                    val shooter = damager.shooter as Player
                    "<dark_red>O disparo certeiro de ${shooter.name} <dark_red>tirou a vida de ${prettyName}!"
                } else {
                    "$prettyName <dark_red>foi derrubado por um disparo!"
                }

            EntityDamageEvent.DamageCause.FIRE,
            EntityDamageEvent.DamageCause.FIRE_TICK ->
                "$prettyName <dark_red>queimou até a morte!"

            EntityDamageEvent.DamageCause.LAVA ->
                "$prettyName <dark_red>tentou nadar na lava!"

            EntityDamageEvent.DamageCause.DROWNING ->
                "$prettyName <dark_red>se afogou!"

            EntityDamageEvent.DamageCause.FALL ->
                "$prettyName <dark_red>caiu de um lugar muito alto!"

            EntityDamageEvent.DamageCause.VOID ->
                "$prettyName <dark_red>despencou no vazio!"

            EntityDamageEvent.DamageCause.SUFFOCATION ->
                "$prettyName <dark_red>ficou sem ar!"

            EntityDamageEvent.DamageCause.CONTACT ->
                "$prettyName <dark_red>foi espetado por um cacto!"

            EntityDamageEvent.DamageCause.STARVATION ->
                "$prettyName <dark_red>morreu de fome!"

            EntityDamageEvent.DamageCause.MAGIC,
            EntityDamageEvent.DamageCause.POISON ->
                "$prettyName <dark_red>foi derrotado pela magia!"

            EntityDamageEvent.DamageCause.WITHER ->
                "$prettyName <dark_red>morreu para um Wither!"

            EntityDamageEvent.DamageCause.LIGHTNING ->
                "$prettyName <dark_red>foi atingido por um raio!"

            EntityDamageEvent.DamageCause.ENTITY_EXPLOSION,
            EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ->
                "$prettyName <dark_red>explodiu em pedacinhos!"

            EntityDamageEvent.DamageCause.CRAMMING ->
                "$prettyName <dark_red>foi esmagado!"

            else -> "$prettyName <dark_red>morreu de forma misteriosa..."
        }

    Bukkit.getOnlinePlayers().forEach { it.sendRichMessage(message) }
    }
}
