package ocean_block.system.islands.island

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import ocean_block.system.islands.position.Position

class RaftPlacementTest : StringSpec({
    "centro do raft fica na superficie do mar" {
        val position = Position.fromIndex(1)
        val center = RaftPlacement.center(position, "world")
        center.x shouldBe position.originX
        center.y shouldBe 62.0
        center.z shouldBe position.originZ
        center.world shouldBe "world"
    }

    "spawn fica em cima do bloco central" {
        val position = Position.fromIndex(1)
        val spawn = RaftPlacement.spawn(position, "world")
        spawn.x shouldBe position.originX + 0.5
        spawn.y shouldBe 63.0
        spawn.z shouldBe position.originZ + 0.5
    }

    "fallback cobre 3x3 em volta do centro" {
        val footprint = RaftPlacement.fallbackFootprint(10, 20)
        footprint.size shouldBe 9
        footprint.toSet() shouldBe (-1..1).flatMap { dx -> (-1..1).map { dz -> (10 + dx) to (20 + dz) } }.toSet()
    }
})
