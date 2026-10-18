package ocean_block.system.drift.debris

import ocean_block.system.drift.collect.CollectorRule
import ocean_block.system.drift.collect.DebrisDelivery
import ocean_block.system.drift.config.DriftSettings
import ocean_block.system.drift.lane.DriftLane
import ocean_block.system.drift.loot.DropRoller
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.island.IslandManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.UUID
import kotlin.math.floor
import kotlin.random.Random

internal class MotionTick(val settings: DriftSettings, val activeLots: Set<UUID>, val nowMillis: Long)

internal object DebrisMotion {

    private const val RAFT_BLOCK_Y = 62

    fun update(debris: Debris, tick: MotionTick): Boolean {
        if (!debris.entity.isValid || debris.entity.isDead) return false
        if (!debris.isFree) return pull(debris)

        val island = IslandManager.islandsById[debris.islandId] ?: return false
        if (island.uuid !in tick.activeLots) return false
        if (DebrisRules.stuckExpired(debris.stuckSinceMillis, tick.nowMillis, tick.settings.stuckSeconds)) return false
        if (!drift(debris, island, tick)) return false

        return !touchCollect(debris, island, tick.settings.touchRadius)
    }

    private fun pull(debris: Debris): Boolean {
        val player = debris.pullTarget?.let(Bukkit::getPlayer) ?: return false
        if (player.world != debris.entity.world) {
            deliver(debris, player)
            return false
        }

        val from = debris.entity.location
        val fraction = DebrisRules.pullFraction(debris.pullTicksLeft)
        val offset = player.location.toVector().subtract(from.toVector()).multiply(fraction)
        debris.entity.teleport(from.add(offset))
        debris.pullTicksLeft -= DebrisRules.TICKS_PER_STEP

        if (debris.pullTicksLeft > 0) return true
        deliver(debris, player)
        return false
    }

    private fun drift(debris: Debris, island: Island, tick: MotionTick): Boolean {
        val settings = tick.settings
        val next = DriftLane.advance(debris.point, settings.direction, DebrisRules.stepDistance(settings.speed))
        val origin = island.position
        if (DriftLane.isPastEnd(next, origin.originX, origin.originZ, settings.direction, settings.laneLength)) return false

        val world = debris.entity.world
        val blockX = floor(next.x).toInt()
        val blockZ = floor(next.z).toInt()
        if (!DebrisEntities.isLoaded(world, blockX, blockZ)) return false

        if (!DebrisRules.isPassable(world.getType(blockX, RAFT_BLOCK_Y, blockZ))) {
            if (debris.stuckSinceMillis == null) debris.stuckSinceMillis = tick.nowMillis
            return true
        }

        debris.stuckSinceMillis = null
        debris.point = next
        debris.entity.teleport(Location(world, next.x, settings.surfaceY, next.z))
        return true
    }

    private fun touchCollect(debris: Debris, island: Island, radius: Double): Boolean {
        val location = debris.entity.location
        val radiusSquared = radius * radius
        val collector = location.getNearbyPlayers(radius).firstOrNull {
            it.location.distanceSquared(location) <= radiusSquared && CollectorRule.canCollect(island, it)
        } ?: return false

        deliver(debris, collector)
        return true
    }

    private fun deliver(debris: Debris, player: Player) {
        DebrisDelivery.deliver(player, DropRoller.roll(debris.type, Random))
    }
}
