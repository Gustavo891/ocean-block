package ocean_block.system.drift.debris

import ocean_block.system.drift.config.DriftSettings
import ocean_block.system.drift.lane.LanePoint
import org.bukkit.World
import kotlin.math.floor

internal sealed interface Passage {
    data object Unloaded : Passage
    data object Blocked : Passage
    data class Open(val depth: Int) : Passage
}

internal object DebrisDive {

    fun passage(debris: Debris, ahead: LanePoint, settings: DriftSettings): Passage {
        val world = debris.entity.world
        val location = debris.entity.location
        val aheadX = floor(ahead.x).toInt()
        val aheadZ = floor(ahead.z).toInt()
        val hereX = location.blockX
        val hereZ = location.blockZ
        if (!DebrisEntities.isLoaded(world, aheadX, aheadZ)) return Passage.Unloaded
        if (!DebrisEntities.isLoaded(world, hereX, hereZ)) return Passage.Unloaded

        val raftY = floor(settings.surfaceY).toInt()
        val depth = DiveRules.targetDepth(
            debris.depth,
            settings.obstacle.maxDepth,
            { passable(world, aheadX, raftY - it, aheadZ) },
            { passable(world, hereX, raftY - it, hereZ) },
        )
        return depth?.let(Passage::Open) ?: Passage.Blocked
    }

    fun approach(debris: Debris, depth: Int, settings: DriftSettings): Boolean {
        val targetY = DiveRules.levelY(depth, settings.surfaceY, debris.type.sink)
        if (DiveRules.atTarget(debris.levelY, targetY)) {
            debris.depth = depth
            debris.levelY = targetY
            return true
        }

        val speed = settings.obstacle.diveSpeed * DebrisRules.TICKS_PER_STEP
        debris.levelY = DiveRules.stepHeight(debris.levelY, targetY, speed)
        return false
    }

    fun hold(debris: Debris, settings: DriftSettings) {
        DebrisEntities.place(debris.entity, poseHere(debris, settings, debris.levelY))
    }

    fun beginFade(debris: Debris) {
        debris.fadeStartTick = debris.ageTicks
        debris.stuckSinceMillis = null
    }

    fun fade(debris: Debris, settings: DriftSettings): Boolean {
        val start = debris.fadeStartTick ?: return false
        val fadeTicks = settings.obstacle.fadeTicks
        if (DiveRules.fadeProgress(debris.ageTicks - start, fadeTicks) >= 1.0) return false

        debris.ageTicks += DebrisRules.TICKS_PER_STEP
        val progress = DiveRules.fadeProgress(debris.ageTicks - start, fadeTicks)
        val pose = poseHere(debris, settings, debris.levelY - DiveRules.FADE_SINK * progress)
        DebrisEntities.place(debris.entity, pose, (debris.type.scale * (1.0 - progress)).toFloat())
        return true
    }

    fun bobScale(debris: Debris, settings: DriftSettings): Double =
        DiveRules.bobScale(debris.levelY, DiveRules.levelY(0, settings.surfaceY, debris.type.sink))

    private fun poseHere(debris: Debris, settings: DriftSettings, levelY: Double): Pose {
        val location = debris.entity.location
        val here = LanePoint(location.x, location.z)
        return DebrisPose.inPlace(here, debris.ageTicks, debris.float, settings, levelY, bobScale(debris, settings))
    }

    private fun passable(world: World, x: Int, y: Int, z: Int): Boolean =
        DebrisRules.isPassable(world.getType(x, y, z))
}
