package ocean_block.system.drift.debris

import ocean_block.system.drift.config.DriftSettings
import ocean_block.system.drift.lane.LanePoint
import kotlin.math.floor

internal object DebrisDive {

    fun passage(debris: Debris, ahead: LanePoint, settings: DriftSettings): Passage {
        val raftY = DiveRules.raftY(settings.surfaceY)
        val here = hereColumn(debris, raftY)
        val front = Column(debris.entity.world, floor(ahead.x).toInt(), floor(ahead.z).toInt(), raftY)
        return DiveRules.passage(
            loaded = front.isLoaded && here.isLoaded,
            depth = debris.depth,
            levelDepth = raftY - floor(debris.levelY).toInt(),
            maxDepth = settings.obstacle.maxDepth,
            here = here::kindAtDepth,
            front = front::kindAtDepth,
        )
    }

    fun hereColumn(debris: Debris, raftY: Int): Column {
        val location = debris.entity.location
        return Column(debris.entity.world, location.blockX, location.blockZ, raftY)
    }

    fun approach(debris: Debris, open: Passage.Open, settings: DriftSettings): Boolean {
        val targetY = DiveRules.levelY(open.depth, open.atSurface, settings.surfaceY, debris.type.sink)
        if (DiveRules.atTarget(debris.levelY, targetY)) {
            debris.depth = open.depth
            debris.atSurface = open.atSurface
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
        when {
            debris.support != Support.FLOAT -> 0.0
            !debris.atSurface -> DiveRules.SUBMERGED_BOB_SCALE
            else -> DiveRules.bobScale(debris.levelY, DiveRules.levelY(debris.depth, true, settings.surfaceY, debris.type.sink))
        }

    private fun poseHere(debris: Debris, settings: DriftSettings, levelY: Double): Pose {
        val location = debris.entity.location
        val here = LanePoint(location.x, location.z)
        val age = if (debris.support == Support.GROUND) debris.restAgeTicks else debris.ageTicks
        return DebrisPose.inPlace(here, age, debris.float, settings, levelY, bobScale(debris, settings))
    }
}
