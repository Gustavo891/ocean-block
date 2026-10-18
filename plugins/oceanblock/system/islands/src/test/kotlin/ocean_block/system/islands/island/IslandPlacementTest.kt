package ocean_block.system.islands.island

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import ocean_block.system.islands.position.Position

class IslandPlacementTest : StringSpec({
    "centro da ilha fica no chao da ilha" {
        val position = Position.fromIndex(1)
        val center = IslandPlacement.center(position, "world")
        center.x shouldBe position.originX
        center.y shouldBe 63.0
        center.z shouldBe position.originZ
        center.world shouldBe "world"
    }

    "spawn fica em cima do bloco central na altura da colagem" {
        val position = Position.fromIndex(1)
        val spawn = IslandPlacement.spawn(position, "world")
        spawn.x shouldBe position.originX + 0.5
        spawn.y shouldBe 64.0
        spawn.z shouldBe position.originZ + 0.5
        spawn.world shouldBe "world"
    }

    "colagem fica um bloco acima do chao" {
        IslandPlacement.PASTE_Y shouldBe 64
        IslandPlacement.GROUND_Y shouldBe 63
    }

    "fallback cobre 3x3 em volta do centro" {
        val footprint = IslandPlacement.fallbackFootprint(10, 20)
        footprint.size shouldBe 9
        footprint.toSet() shouldBe (-1..1).flatMap { dx -> (-1..1).map { dz -> (10 + dx) to (20 + dz) } }.toSet()
    }
})
