package ocean_block.system.drift.debris

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

object FloatMotion {

    const val WAKE_INTERVAL_TICKS = 10

    private const val TICKS_PER_SECOND = 20.0
    private const val STEPS_PER_SECOND = TICKS_PER_SECOND / DebrisRules.TICKS_PER_STEP

    fun bob(ageTicks: Int, phase: Double, amplitude: Double, periodTicks: Int): Double =
        wave(ageTicks, phase, amplitude, periodTicks)

    fun meander(ageTicks: Int, phase: Double, amplitude: Double, periodTicks: Int): Double =
        wave(ageTicks, phase, amplitude, periodTicks)

    fun tilt(ageTicks: Int, phase: Double, degrees: Double, periodTicks: Int): Double =
        wave(ageTicks, phase, degrees, periodTicks)

    fun yaw(ageTicks: Int, degreesPerSecond: Double, direction: Int): Double =
        direction * degreesPerSecond * ageTicks / TICKS_PER_SECOND

    fun speedFactor(random: Random, variation: Double): Double =
        if (variation <= 0.0) 1.0 else random.nextDouble(1.0 - variation, 1.0 + variation)

    fun wakeDue(ageTicks: Int): Boolean = ageTicks % WAKE_INTERVAL_TICKS == 0

    fun streaksThisStep(perSecond: Double, random: Random): Int {
        if (perSecond <= 0.0) return 0
        val perStep = perSecond / STEPS_PER_SECOND
        val whole = floor(perStep)
        return whole.toInt() + if (random.nextDouble() < perStep - whole) 1 else 0
    }

    private fun wave(ageTicks: Int, phase: Double, amplitude: Double, periodTicks: Int): Double =
        amplitude * sin(2 * PI * ageTicks / periodTicks.coerceAtLeast(1) + phase)
}
