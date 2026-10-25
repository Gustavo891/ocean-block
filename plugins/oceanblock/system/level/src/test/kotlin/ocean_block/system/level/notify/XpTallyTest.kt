package ocean_block.system.level.notify

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class XpTallyTest : StringSpec({

    "ganhos dentro da janela se somam e fora dela recomecam" {
        val tally = XpTally(60)

        tally.add(5, 0) shouldBe 5
        tally.add(3, 10) shouldBe 8
        tally.add(4, 100) shouldBe 4
    }
})
