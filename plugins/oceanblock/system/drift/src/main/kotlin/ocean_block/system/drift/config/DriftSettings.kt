package ocean_block.system.drift.config

data class DriftSettings(
    val direction: CurrentDirection,
    val speed: Double,
    val laneLength: Double,
    val laneWidth: Double,
    val laneMinWidth: Double,
    val surfaceY: Double,
    val spawnIntervalTicks: Long,
    val maxPerLot: Int,
    val stuckSeconds: Int,
    val hookRadius: Double,
    val touchRadius: Double,
    val netRadius: Double,
    val streaks: Boolean,
    val streaksPerSecond: Double,
    val animation: AnimationSettings,
    val obstacle: ObstacleSettings,
)
