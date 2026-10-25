package ocean_block.system.level.notify

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class XpTextTest : StringSpec({

    "o ganho perto da mira mostra o total somado" {
        XpText.gain(8) shouldBe "<green>+8</green> <white>XP</white>"
    }
})
