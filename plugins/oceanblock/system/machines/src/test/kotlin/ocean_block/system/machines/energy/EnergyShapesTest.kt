package ocean_block.system.machines.energy

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

class EnergyShapesTest : StringSpec({

    "rastro marca pontos a cada passo ate o fim" {
        EnergyShapes.trail(1.0, 0.25, 0.0) shouldBe listOf(0.0, 0.25, 0.5, 0.75, 1.0)
    }

    "fase empurra os pontos e nenhum passa do fim" {
        EnergyShapes.trail(1.0, 0.5, 0.2).map { Math.round(it * 10) / 10.0 } shouldBe listOf(0.2, 0.7)
    }

    "fase do tick anda um passo a cada ciclo" {
        EnergyShapes.phase(0, 4, 0.4) shouldBe 0.0
        EnergyShapes.phase(1, 4, 0.4) shouldBe (0.1 plusOrMinus 1e-9)
        EnergyShapes.phase(5, 4, 0.4) shouldBe (0.1 plusOrMinus 1e-9)
    }

    "circulo fica no raio e fecha a volta" {
        val points = EnergyShapes.circle(8.0, 64)
        points.size shouldBe 64
        points.forEach { (x, z) -> Math.hypot(x, z) shouldBe (8.0 plusOrMinus 1e-9) }
        points.first().first shouldBe (8.0 plusOrMinus 1e-9)
    }

    "trilha vazia sem distancia" {
        EnergyShapes.trail(0.0, 0.25, 0.0) shouldBe listOf(0.0)
    }
})
