package ocean_block.system.drift.debris

import ocean_block.system.drift.config.DriftSettings
import ocean_block.system.drift.lane.DriftLane
import ocean_block.system.drift.lane.LanePoint
import org.bukkit.Particle
import org.bukkit.World
import kotlin.math.floor
import kotlin.random.Random

internal object DebrisParticles {

    private const val WAKE_BEHIND = 0.4
    private const val WAKE_LIFT = 0.05
    private const val WAKE_SPLASH_COUNT = 3
    private const val WAKE_SPREAD = 0.1
    private const val WAKE_POP_COUNT = 1
    private const val BUBBLE_COUNT = 2
    private const val STREAK_LIFT = 0.1
    private const val STREAK_SPEED = 0.05

    fun wake(world: World, point: LanePoint, waterline: Double, settings: DriftSettings) {
        val behind = DriftLane.advance(point, settings.direction, -WAKE_BEHIND)
        val y = waterline + WAKE_LIFT
        world.spawnParticle(Particle.SPLASH, behind.x, y, behind.z, WAKE_SPLASH_COUNT, WAKE_SPREAD, 0.0, WAKE_SPREAD, 0.0)
        world.spawnParticle(Particle.BUBBLE_POP, behind.x, y, behind.z, WAKE_POP_COUNT, 0.0, 0.0, 0.0, 0.0)
    }

    fun bubbles(world: World, point: LanePoint, y: Double, settings: DriftSettings) {
        val behind = DriftLane.advance(point, settings.direction, -WAKE_BEHIND)
        world.spawnParticle(Particle.BUBBLE, behind.x, y, behind.z, BUBBLE_COUNT, WAKE_SPREAD, WAKE_SPREAD, WAKE_SPREAD, 0.0)
    }

    fun streaks(world: World, centerX: Double, centerZ: Double, settings: DriftSettings, random: Random) {
        repeat(FloatMotion.streaksThisStep(settings.streaksPerSecond, random)) {
            streak(world, randomLanePoint(centerX, centerZ, settings, random), settings)
        }
    }

    private fun streak(world: World, point: LanePoint, settings: DriftSettings) {
        if (!DebrisEntities.isLoaded(world, floor(point.x).toInt(), floor(point.z).toInt())) return

        val direction = settings.direction
        world.spawnParticle(
            Particle.FISHING,
            point.x,
            settings.surfaceY + STREAK_LIFT,
            point.z,
            0,
            direction.dx.toDouble(),
            0.0,
            direction.dz.toDouble(),
            STREAK_SPEED,
        )
    }

    private fun randomLanePoint(centerX: Double, centerZ: Double, settings: DriftSettings, random: Random): LanePoint {
        val half = settings.laneLength / 2.0
        val along = random.nextDouble(-half, half)
        val lateral = DriftLane.lateralOffset(random, settings.laneMinWidth, settings.laneWidth)
        return DriftLane.pointAt(centerX, centerZ, settings.direction, along, lateral)
    }
}
