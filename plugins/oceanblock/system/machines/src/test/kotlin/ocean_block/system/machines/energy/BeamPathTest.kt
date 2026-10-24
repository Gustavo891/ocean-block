package ocean_block.system.machines.energy

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.random.Random

class BeamPathTest : StringSpec({

    val from = BeamPoint(0.0, 0.0, 0.0)
    val to = BeamPoint(8.0, 2.0, 0.0)

    "raio comeca e termina nos blocos" {
        val path = BeamPath.points(from, to, 6, 0.3, Random(1))
        path.size shouldBe 7
        path.first() shouldBe from
        path.last() shouldBe to
    }

    "desvio de cada ponto fica dentro do limite" {
        val path = BeamPath.points(from, to, 6, 0.3, Random(2))
        path.drop(1).dropLast(1).forEachIndexed { index, point ->
            val t = (index + 1) / 6.0
            val straight = BeamPoint(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t, from.z + (to.z - from.z) * t)
            point.distanceTo(straight) shouldBeLessThanOrEqual 0.3 * Math.sqrt(3.0) + 1e-9
        }
    }

    "sem desvio e uma linha reta" {
        BeamPath.points(from, to, 4, 0.0, Random(3))[2] shouldBe BeamPoint(4.0, 1.0, 0.0)
    }

    "quantidade de segmentos cresce com a distancia" {
        BeamPath.segmentsFor(1.0) shouldBe 2
        BeamPath.segmentsFor(8.0) shouldBe 8
        BeamPath.segmentsFor(40.0) shouldBe 12
    }

    "distancia entre pontos" {
        from.distanceTo(BeamPoint(3.0, 4.0, 0.0)) shouldBe (5.0 plusOrMinus 1e-9)
    }
})
