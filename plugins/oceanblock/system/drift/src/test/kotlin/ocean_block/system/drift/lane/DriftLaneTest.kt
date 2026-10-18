package ocean_block.system.drift.lane

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import ocean_block.system.drift.config.CurrentDirection

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

    "fim da trilha" {
        val east = CurrentDirection.EAST

        DriftLane.isPastEnd(LanePoint(1040.1, 0.0), 1000.0, 0.0, east, 80.0) shouldBe true
        DriftLane.isPastEnd(LanePoint(1039.9, 0.0), 1000.0, 0.0, east, 80.0) shouldBe false
        DriftLane.isPastEnd(LanePoint(960.0, 0.0), 1000.0, 0.0, east, 80.0) shouldBe false
    }
})
