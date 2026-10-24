package ocean_block.system.machines.energy

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

class EnergyShapesTest : StringSpec({

    "circulo fica no raio e fecha a volta" {
        val points = EnergyShapes.circle(8.0, 64)
        points.size shouldBe 64
        points.forEach { (x, z) -> Math.hypot(x, z) shouldBe (8.0 plusOrMinus 1e-9) }
        points.first().first shouldBe (8.0 plusOrMinus 1e-9)
    }
})
