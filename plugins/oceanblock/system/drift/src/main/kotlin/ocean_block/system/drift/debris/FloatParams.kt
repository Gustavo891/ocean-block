package ocean_block.system.drift.debris

import kotlin.math.PI
import kotlin.random.Random

data class FloatParams(
    val bobPhase: Double,
    val meanderPhase: Double,
    val tiltPhase: Double,
    val spinDirection: Int,
    val speedFactor: Double,
) {
    companion object {
        fun roll(random: Random, speedVariation: Double): FloatParams =
            FloatParams(
                bobPhase = random.nextDouble(0.0, 2 * PI),
                meanderPhase = random.nextDouble(0.0, 2 * PI),
                tiltPhase = random.nextDouble(0.0, 2 * PI),
                spinDirection = if (random.nextBoolean()) 1 else -1,
                speedFactor = FloatMotion.speedFactor(random, speedVariation),
            )
    }
}
