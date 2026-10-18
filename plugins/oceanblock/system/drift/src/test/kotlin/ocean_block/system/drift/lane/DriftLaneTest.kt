package ocean_block.system.drift.lane

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import ocean_block.system.drift.config.CurrentDirection
import kotlin.math.abs
import kotlin.random.Random

class DriftLaneTest : StringSpec({

    "EAST nasce a oeste do centro com lateral em z" {
        DriftLane.spawnPoint(1000.0, 0.0, CurrentDirection.EAST, 80.0, 5.0) shouldBe LanePoint(960.0, 5.0)
    }

    "NORTH nasce ao sul" {
        DriftLane.spawnPoint(0.0, 0.0, CurrentDirection.NORTH, 80.0, 3.0) shouldBe LanePoint(3.0, 40.0)
    }

    "advance anda na direcao" {
        val moved = DriftLane.advance(LanePoint(960.0, 5.0), CurrentDirection.EAST, 0.1)

        moved shouldBe LanePoint(960.1, 5.0)
    }

    "shift desloca na perpendicular da corrente" {
        DriftLane.shift(LanePoint(10.0, 5.0), CurrentDirection.EAST, 0.4) shouldBe LanePoint(10.0, 5.4)
        DriftLane.shift(LanePoint(10.0, 5.0), CurrentDirection.NORTH, 2.0) shouldBe LanePoint(12.0, 5.0)
    }

    "pointAt mede ao longo e ao lado da corrente a partir do centro" {
        DriftLane.pointAt(100.0, 0.0, CurrentDirection.EAST, 10.0, -3.0) shouldBe LanePoint(110.0, -3.0)
        DriftLane.pointAt(0.0, 0.0, CurrentDirection.SOUTH, 4.0, 1.0) shouldBe LanePoint(-1.0, 4.0)
    }

    "lateral fica entre o minimo e o maximo dos dois lados da ilha" {
        val random = Random(42)
        val offsets = List(2000) { DriftLane.lateralOffset(random, 10.0, 14.0) }

        offsets.forEach {
            abs(it) shouldBeGreaterThanOrEqual 10.0
            abs(it) shouldBeLessThanOrEqual 14.0
        }
        offsets.any { it > 0.0 } shouldBe true
        offsets.any { it < 0.0 } shouldBe true
    }

    "lateral com minimo zero cobre a faixa inteira como antes" {
        val random = Random(7)
        val offsets = List(4000) { DriftLane.lateralOffset(random, 0.0, 10.0) }

        offsets.forEach { abs(it) shouldBeLessThanOrEqual 10.0 }
        offsets.any { abs(it) < 1.0 } shouldBe true
        offsets.any { it > 9.0 } shouldBe true
        offsets.any { it < -9.0 } shouldBe true
    }

    "lateral com largura zero fica no centro" {
        DriftLane.lateralOffset(Random(1), 0.0, 0.0) shouldBe 0.0
    }

    "lateral com minimo igual ao maximo fica na borda" {
        val random = Random(3)
        List(100) { abs(DriftLane.lateralOffset(random, 12.0, 12.0)) }.toSet() shouldBe setOf(12.0)
    }

    "fim da trilha" {
        val east = CurrentDirection.EAST

        DriftLane.isPastEnd(LanePoint(1040.1, 0.0), 1000.0, 0.0, east, 80.0) shouldBe true
        DriftLane.isPastEnd(LanePoint(1039.9, 0.0), 1000.0, 0.0, east, 80.0) shouldBe false
        DriftLane.isPastEnd(LanePoint(960.0, 0.0), 1000.0, 0.0, east, 80.0) shouldBe false
    }
})
