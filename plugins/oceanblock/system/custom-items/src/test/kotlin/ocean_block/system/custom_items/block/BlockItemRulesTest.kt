package ocean_block.system.custom_items.block

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class BlockItemRulesTest : StringSpec({

    "item de bloco com material antigo precisa ser trocado" {
        BlockItemRules.isOutdated(itemMaterial = "NOTE_BLOCK", blockMaterial = "DAYLIGHT_DETECTOR") shouldBe true
        BlockItemRules.isOutdated(itemMaterial = "LOOM", blockMaterial = "BARRIER") shouldBe true
    }

    "item com o mesmo material do bloco fica como esta" {
        BlockItemRules.isOutdated(itemMaterial = "NOTE_BLOCK", blockMaterial = "NOTE_BLOCK") shouldBe false
    }
})
