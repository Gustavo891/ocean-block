package ocean_block.system.pirate_raid.raid

import ocean_block.system.pirate_raid.config.RaidConfig
import ocean_block.system.pirate_raid.rules.BoatPath
import ocean_block.system.pirate_raid.rules.Vec
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import org.bukkit.util.Vector
import kotlin.math.atan2
import kotlin.math.hypot

object BoatPilot {

    private const val LOOK_AHEAD = 1.6
    private const val STILL_EPSILON = 0.02
    private const val REMOVE_AFTER_TICKS = 100L
    private const val LEAP_SPEED = 0.45
    private const val LEAP_UP = 0.35

    fun tick(raid: ActiveRaid, config: RaidConfig, now: Long) {
        raid.boats.removeIf { boat -> step(boat, raid, config, now) }
    }

    fun removeAll(raid: ActiveRaid) {
        raid.boats.forEach { it.boat.remove() }
        raid.boats.clear()
    }

    private fun step(raidBoat: RaidBoat, raid: ActiveRaid, config: RaidConfig, now: Long): Boolean {
        val boat = raidBoat.boat
        if (!boat.isValid) return true
        val landedAt = raidBoat.landedAtTick
        if (landedAt != null) {
            if (now - landedAt < REMOVE_AFTER_TICKS) return false
            boat.world.spawnParticle(Particle.SPLASH, boat.location, 30, 0.6, 0.2, 0.6)
            boat.remove()
            return true
        }
        val position = Vec(boat.location.x, boat.location.z)
        val center = Vec(raid.center.x, raid.center.z)
        val moved = hypot(position.x - raidBoat.lastX, position.z - raidBoat.lastZ)
        raidBoat.stillTicks = if (moved < STILL_EPSILON) raidBoat.stillTicks + 1 else 0
        raidBoat.lastX = position.x
        raidBoat.lastZ = position.z
        val velocity = BoatPath.step(position, center, config.boatSpeed)
        val distance = BoatPath.distance(position, center)
        if (BoatPath.landed(distance, config.landingDistance, blockedAhead(boat.location, velocity), raidBoat.stillTicks)) {
            land(raidBoat, raid, now)
            return false
        }
        boat.velocity = Vector(velocity.x, boat.velocity.y, velocity.z)
        boat.setRotation(Math.toDegrees(atan2(-velocity.x, velocity.z)).toFloat(), 0f)
        return false
    }

    private fun blockedAhead(location: Location, velocity: Vec): Boolean {
        val length = hypot(velocity.x, velocity.z)
        if (length == 0.0) return false
        val ahead = location.clone().add(velocity.x / length * LOOK_AHEAD, 0.0, velocity.z / length * LOOK_AHEAD)
        return ahead.block.type.isSolid || ahead.clone().add(0.0, 1.0, 0.0).block.type.isSolid
    }

    private fun land(raidBoat: RaidBoat, raid: ActiveRaid, now: Long) {
        val boat = raidBoat.boat
        raidBoat.landedAtTick = now
        val passengers = boat.passengers.toList()
        boat.eject()
        boat.velocity = Vector(0, 0, 0)
        passengers.filterIsInstance<LivingEntity>().forEach { pirate ->
            val direction = raid.center.toVector().subtract(pirate.location.toVector()).setY(0)
            if (direction.lengthSquared() > 0) direction.normalize().multiply(LEAP_SPEED)
            pirate.velocity = direction.setY(LEAP_UP)
        }
    }
}
