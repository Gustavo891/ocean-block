package ocean_block.system.drift.debris

import ocean_block.system.drift.config.DriftSettings
import ocean_block.system.drift.lane.LanePoint
import kotlin.math.floor

internal sealed interface Passage {
    data object Unloaded : Passage
    data object Unsupported : Passage
    data object Drop : Passage
    data object Blocked : Passage
    data class Open(val depth: Int, val atSurface: Boolean) : Passage
}

internal object DebrisDive {

    private const val SURFACE_SCAN_MARGIN = 4

    fun passage(debris: Debris, ahead: LanePoint, settings: DriftSettings): Passage {
        val raftY = DiveRules.raftY(settings.surfaceY)
        val here = hereColumn(debris, raftY)
        val front = Column(debris.entity.world, floor(ahead.x).toInt(), floor(ahead.z).toInt(), raftY)
        if (!front.isLoaded || !here.isLoaded) return Passage.Unloaded
        if (!here.isWaterAtDepth(debris.depth)) return Passage.Unsupported
        if (front.kindAt(raftY - debris.depth) == BlockKind.AIR) return Passage.Drop

        return open(debris, here, front, settings.obstacle.maxDepth) ?: Passage.Blocked
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

    private fun open(debris: Debris, here: Column, front: Column, maxDepth: Int): Passage.Open? {
        val depth = DiveRules.targetDepth(
            debris.depth,
            maxOf(maxDepth, debris.depth),
            front::isWaterAtDepth,
            here::isWaterAtDepth,
        ) ?: return null
        val scan = maxOf(maxDepth + SURFACE_SCAN_MARGIN, depth)
        val surface = GravityRules.surfaceDepth(scan, front::isWaterAtDepth, front::isSolidAtDepth)
        return Passage.Open(depth, surface == depth)
    }

    private fun poseHere(debris: Debris, settings: DriftSettings, levelY: Double): Pose {
        val location = debris.entity.location
        val here = LanePoint(location.x, location.z)
        val age = if (debris.support == Support.GROUND) debris.restAgeTicks else debris.ageTicks
        return DebrisPose.inPlace(here, age, debris.float, settings, levelY, bobScale(debris, settings))
    }
}
