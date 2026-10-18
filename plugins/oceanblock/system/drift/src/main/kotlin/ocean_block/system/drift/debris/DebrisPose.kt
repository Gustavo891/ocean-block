package ocean_block.system.drift.debris

import ocean_block.system.drift.config.DriftSettings
import ocean_block.system.drift.lane.DriftLane
import ocean_block.system.drift.lane.LanePoint
import org.joml.Quaternionf
import kotlin.math.PI

internal data class Pose(val point: LanePoint, val y: Double, val rotation: Quaternionf)

internal object DebrisPose {

    private const val ROLL_PERIOD_RATIO = 1.6
    private const val ROLL_PHASE_OFFSET = PI / 2
    private const val FULL_TURN_DEGREES = 360.0

    fun shifted(base: LanePoint, ageTicks: Int, params: FloatParams, settings: DriftSettings): LanePoint {
        val animation = settings.animation
        val lateral = FloatMotion.meander(ageTicks, params.meanderPhase, animation.meander, animation.meanderPeriodTicks)
        return DriftLane.shift(base, settings.direction, lateral)
    }

    fun along(base: LanePoint, ageTicks: Int, params: FloatParams, settings: DriftSettings, levelY: Double): Pose =
        inPlace(shifted(base, ageTicks, params, settings), ageTicks, params, settings, levelY, 1.0)

    fun inPlace(
        point: LanePoint,
        ageTicks: Int,
        params: FloatParams,
        settings: DriftSettings,
        levelY: Double,
        bobScale: Double,
    ): Pose {
        val animation = settings.animation
        val bob = FloatMotion.bob(ageTicks, params.bobPhase, animation.bobAmplitude, animation.bobPeriodTicks)
        return Pose(point, levelY + bob * bobScale, rotation(ageTicks, params, settings))
    }

    private fun rotation(ageTicks: Int, params: FloatParams, settings: DriftSettings): Quaternionf {
        val animation = settings.animation
        val rollPeriod = (animation.bobPeriodTicks * ROLL_PERIOD_RATIO).toInt()
        val yaw = FloatMotion.yaw(ageTicks, animation.spinDegreesPerSecond, params.spinDirection) % FULL_TURN_DEGREES
        val pitch = FloatMotion.tilt(ageTicks, params.tiltPhase, animation.tiltDegrees, animation.bobPeriodTicks)
        val roll = FloatMotion.tilt(ageTicks, params.tiltPhase + ROLL_PHASE_OFFSET, animation.tiltDegrees, rollPeriod)
        return Quaternionf()
            .rotateY(Math.toRadians(yaw).toFloat())
            .rotateX(Math.toRadians(pitch).toFloat())
            .rotateZ(Math.toRadians(roll).toFloat())
    }
}
