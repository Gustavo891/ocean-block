package ocean_block.system.machines.energy

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

class BeamPathTest : StringSpec({

    "linha reta vai de uma ponta a outra com o espacamento pedido" {
        val line = BeamPath.line(BeamPoint(0.0, 0.0, 0.0), BeamPoint(1.0, 0.0, 0.0), 0.25)
        line shouldBe listOf(0.0, 0.25, 0.5, 0.75, 1.0).map { BeamPoint(it, 0.0, 0.0) }
    }

    "todos os pontos ficam na reta" {
        val line = BeamPath.line(BeamPoint(0.0, 1.0, 0.0), BeamPoint(4.0, 3.0, 0.0), 0.15)
        line.forEach { point -> point.y shouldBe (1.0 + point.x / 2 plusOrMinus 1e-9) }
        line.last() shouldBe BeamPoint(4.0, 3.0, 0.0)
    }

    "distancia zero ainda desenha as pontas" {
        BeamPath.line(BeamPoint(1.0, 1.0, 1.0), BeamPoint(1.0, 1.0, 1.0), 0.15).size shouldBe 2
    }

    "distancia entre pontos" {
        BeamPoint(0.0, 0.0, 0.0).distanceTo(BeamPoint(3.0, 4.0, 0.0)) shouldBe (5.0 plusOrMinus 1e-9)
    }
})
