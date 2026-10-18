package ocean_block.system.drift.lane

import ocean_block.system.drift.config.CurrentDirection

object DriftLane {

    fun spawnPoint(
        originX: Double,
        originZ: Double,
        direction: CurrentDirection,
        laneLength: Double,
        lateral: Double,
    ): LanePoint {
        val half = laneLength / 2.0
        val perpX = -direction.dz
        val perpZ = direction.dx
        return LanePoint(
            x = originX - direction.dx * half + perpX * lateral,
            z = originZ - direction.dz * half + perpZ * lateral,
        )
    }

    fun advance(point: LanePoint, direction: CurrentDirection, distance: Double): LanePoint =
        LanePoint(point.x + direction.dx * distance, point.z + direction.dz * distance)

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
