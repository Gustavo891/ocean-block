package ocean_block.system.drift.debris

import ocean_block.system.drift.collect.CollectorRule
import ocean_block.system.drift.collect.DebrisDelivery
import ocean_block.system.drift.config.DriftSettings
import ocean_block.system.drift.lane.DriftLane
import ocean_block.system.drift.lane.LanePoint
import ocean_block.system.drift.loot.DropRoller
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.island.IslandManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import kotlin.random.Random

internal class MotionTick(val settings: DriftSettings, val activeLots: Set<UUID>, val nowMillis: Long)

internal object DebrisMotion {

    fun update(debris: Debris, tick: MotionTick): Boolean {
        if (!debris.entity.isValid || debris.entity.isDead) return false
        if (!debris.isFree) return pull(debris)

        val island = IslandManager.islandsById[debris.islandId] ?: return false
        if (island.uuid !in tick.activeLots) return false
        if (debris.isFading) return DebrisDive.fade(debris, tick.settings)
        if (DebrisRules.stuckExpired(debris.stuckSinceMillis, tick.nowMillis, tick.settings.stuckSeconds)) {
            DebrisDive.beginFade(debris)
            return DebrisDive.fade(debris, tick.settings)
        }
        if (!move(debris, island, tick)) return false

        return !touchCollect(debris, island, tick.settings.touchRadius)
    }

    private fun move(debris: Debris, island: Island, tick: MotionTick): Boolean =
        when (debris.support) {
            Support.FLOAT -> drift(debris, island, tick)
            Support.FALL -> DebrisFall.fall(debris, tick.settings, tick.nowMillis)
            Support.GROUND -> DebrisFall.rest(debris, tick.settings, tick.nowMillis)
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
        val step = DebrisRules.stepDistance(settings.speed) * debris.float.speedFactor
        val next = DriftLane.advance(debris.point, settings.direction, step)
        debris.ageTicks += DebrisRules.TICKS_PER_STEP
        val point = DebrisPose.shifted(next, debris.ageTicks, debris.float, settings)

        val origin = island.position
        val centerX = origin.originX + DriftLane.BLOCK_CENTER
        val centerZ = origin.originZ + DriftLane.BLOCK_CENTER
        if (DriftLane.isPastEnd(point, centerX, centerZ, settings.direction, settings.laneLength)) return false

        return when (val passage = DebrisDive.passage(debris, point, settings)) {
            Passage.Unloaded -> false
            Passage.Unsupported -> startFalling(debris, settings)
            Passage.Drop -> {
                DebrisFall.begin(debris)
                advanceTo(debris, next, point, settings)
            }
            Passage.Blocked -> stall(debris, settings, tick.nowMillis)
            is Passage.Open -> travel(debris, next, point, passage, settings)
        }
    }

    private fun startFalling(debris: Debris, settings: DriftSettings): Boolean {
        DebrisFall.begin(debris)
        DebrisDive.hold(debris, settings)
        return true
    }

    private fun stall(debris: Debris, settings: DriftSettings, nowMillis: Long): Boolean {
        if (debris.stuckSinceMillis == null) debris.stuckSinceMillis = nowMillis
        DebrisDive.hold(debris, settings)
        return true
    }

    private fun travel(debris: Debris, next: LanePoint, point: LanePoint, open: Passage.Open, settings: DriftSettings): Boolean {
        if (DebrisDive.approach(debris, open, settings)) return advanceTo(debris, next, point, settings)

        DebrisDive.hold(debris, settings)
        return true
    }

    private fun advanceTo(debris: Debris, next: LanePoint, point: LanePoint, settings: DriftSettings): Boolean {
        debris.stuckSinceMillis = null
        debris.point = next
        val bobScale = DebrisDive.bobScale(debris, settings)
        DebrisEntities.place(debris.entity, DebrisPose.inPlace(point, debris.ageTicks, debris.float, settings, debris.levelY, bobScale))
        if (debris.support == Support.FLOAT && settings.animation.wake && FloatMotion.wakeDue(debris.ageTicks)) {
            trail(debris, point, settings)
        }
        return true
    }

    private fun trail(debris: Debris, point: LanePoint, settings: DriftSettings) {
        val world = debris.entity.world
        if (debris.atSurface) DebrisParticles.wake(world, point, settings.surfaceY - debris.depth, settings)
        else DebrisParticles.bubbles(world, point, debris.levelY, settings)
    }

    private fun touchCollect(debris: Debris, island: Island, radius: Double): Boolean {
        if (!debris.isCollectable) return false

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
