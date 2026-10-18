package ocean_block.system.drift.config

data class AnimationSettings(
    val bobAmplitude: Double,
    val bobPeriodTicks: Int,
    val tiltDegrees: Double,
    val spinDegreesPerSecond: Double,
    val meander: Double,
    val meanderPeriodTicks: Int,
    val speedVariation: Double,
    val wake: Boolean,
)
