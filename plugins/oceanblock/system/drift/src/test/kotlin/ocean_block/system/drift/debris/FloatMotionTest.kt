package ocean_block.system.drift.debris

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import kotlin.math.PI
import kotlin.math.abs
import kotlin.random.Random

class FloatMotionTest : StringSpec({

    "ondas valem zero na idade zero com fase zero" {
        FloatMotion.bob(0, 0.0, 0.06, 50) shouldBe (0.0 plusOrMinus 1e-12)
        FloatMotion.meander(0, 0.0, 0.4, 120) shouldBe (0.0 plusOrMinus 1e-12)
        FloatMotion.tilt(0, 0.0, 8.0, 50) shouldBe (0.0 plusOrMinus 1e-12)
    }

    "ondas atingem a amplitude em um quarto do periodo" {
        FloatMotion.bob(10, 0.0, 0.06, 40) shouldBe (0.06 plusOrMinus 1e-9)
        FloatMotion.meander(30, 0.0, 0.4, 120) shouldBe (0.4 plusOrMinus 1e-9)
        FloatMotion.tilt(10, 0.0, 8.0, 40) shouldBe (8.0 plusOrMinus 1e-9)
    }

    "fase desloca a onda" {
        FloatMotion.bob(0, PI / 2, 0.06, 50) shouldBe (0.06 plusOrMinus 1e-9)
    }

    "ondas nunca passam da amplitude" {
        val random = Random(42)
        repeat(1000) {
            val age = random.nextInt(0, 1_000_000)
            val phase = random.nextDouble(0.0, 2 * PI)
            abs(FloatMotion.bob(age, phase, 0.06, 50)) shouldBeLessThanOrEqual 0.06
            abs(FloatMotion.meander(age, phase, 0.4, 120)) shouldBeLessThanOrEqual 0.4
            abs(FloatMotion.tilt(age, phase, 8.0, 50)) shouldBeLessThanOrEqual 8.0
        }
    }

    "giro cresce linear e inverte com direcao negativa" {
        FloatMotion.yaw(0, 10.0, 1) shouldBe (0.0 plusOrMinus 1e-12)
        FloatMotion.yaw(20, 10.0, 1) shouldBe (10.0 plusOrMinus 1e-9)
        FloatMotion.yaw(40, 10.0, 1) shouldBe (20.0 plusOrMinus 1e-9)
        FloatMotion.yaw(40, 10.0, -1) shouldBe (-20.0 plusOrMinus 1e-9)
    }

    "fator de velocidade fica no intervalo" {
        val random = Random(7)
        repeat(1000) {
            val factor = FloatMotion.speedFactor(random, 0.2)
            factor shouldBeGreaterThanOrEqual 0.8
            factor shouldBeLessThanOrEqual 1.2
        }
    }

    "sem variacao o fator e um" {
        FloatMotion.speedFactor(Random(1), 0.0) shouldBe 1.0
    }

    "rastro a cada dez ticks" {
        FloatMotion.wakeDue(0) shouldBe true
        FloatMotion.wakeDue(10) shouldBe true
        FloatMotion.wakeDue(20) shouldBe true
        FloatMotion.wakeDue(5) shouldBe false
        FloatMotion.wakeDue(12) shouldBe false
    }

    "riscos por passo seguem a media por segundo" {
        val random = Random(3)
        var total = 0
        repeat(1000) {
            val count = FloatMotion.streaksThisStep(6.0, random)
            count shouldBeInRange 0..1
            total += count
        }
        abs(total / 100.0 - 6.0) shouldBeLessThan 0.6
        FloatMotion.streaksThisStep(0.0, random) shouldBe 0
        FloatMotion.streaksThisStep(20.0, random) shouldBe 2
    }
})
