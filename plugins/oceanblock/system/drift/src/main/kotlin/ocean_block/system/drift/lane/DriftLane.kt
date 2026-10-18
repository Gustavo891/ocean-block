package ocean_block.system.drift.lane

import ocean_block.system.drift.config.CurrentDirection
import kotlin.random.Random

object DriftLane {

    const val BLOCK_CENTER = 0.5

    fun spawnPoint(
        originX: Double,
        originZ: Double,
        direction: CurrentDirection,
        laneLength: Double,
        lateral: Double,
    ): LanePoint = pointAt(originX, originZ, direction, -laneLength / 2.0, lateral)

    fun lateralOffset(random: Random, min: Double, max: Double): Double {
        if (max <= 0.0) return 0.0
        val low = min.coerceIn(0.0, max)
        val magnitude = if (low >= max) max else random.nextDouble(low, max)
        return if (random.nextBoolean()) magnitude else -magnitude
    }

    fun pointAt(
        originX: Double,
        originZ: Double,
        direction: CurrentDirection,
        along: Double,
        lateral: Double,
    ): LanePoint = shift(advance(LanePoint(originX, originZ), direction, along), direction, lateral)

    fun advance(point: LanePoint, direction: CurrentDirection, distance: Double): LanePoint =
        LanePoint(point.x + direction.dx * distance, point.z + direction.dz * distance)

    fun shift(point: LanePoint, direction: CurrentDirection, lateral: Double): LanePoint =
        LanePoint(point.x - direction.dz * lateral, point.z + direction.dx * lateral)

    fun isPastEnd(
        point: LanePoint,
        originX: Double,
        originZ: Double,
        direction: CurrentDirection,
        laneLength: Double,
    ): Boolean {
        val along = (point.x - originX) * direction.dx + (point.z - originZ) * direction.dz
        return along > laneLength / 2.0
    }
}
