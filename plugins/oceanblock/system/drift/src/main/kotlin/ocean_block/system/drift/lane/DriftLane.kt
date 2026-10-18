package ocean_block.system.drift.lane

import ocean_block.system.drift.config.CurrentDirection

object DriftLane {

    const val BLOCK_CENTER = 0.5

    fun spawnPoint(
        originX: Double,
        originZ: Double,
        direction: CurrentDirection,
        laneLength: Double,
        lateral: Double,
    ): LanePoint = pointAt(originX, originZ, direction, -laneLength / 2.0, lateral)

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
