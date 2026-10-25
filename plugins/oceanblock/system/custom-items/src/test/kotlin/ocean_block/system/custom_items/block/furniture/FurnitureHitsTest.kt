package ocean_block.system.custom_items.block.furniture

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class FurnitureHitsTest : StringSpec({

    "golpes para quebrar valem dentro da janela" {
        FurnitureHits.next(previous = null, now = 100) shouldBe 1
        FurnitureHits.next(previous = FurnitureHit(2, 90), now = 100) shouldBe 3
        FurnitureHits.next(previous = FurnitureHit(2, 50), now = 100) shouldBe 1
        FurnitureHits.TO_BREAK shouldBe 3
    }
})
