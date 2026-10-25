package ocean_block.system.machines

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ItemLabelsTest : StringSpec({

    "icone usa o nome do item e corrige os que nao tem textura propria" {
        ItemLabels.icon("RAW_IRON") shouldBe "raw_iron"
        ItemLabels.icon("OAK_WOOD") shouldBe "oak_log"
        ItemLabels.icon("DRIED_KELP_BLOCK") shouldBe "dried_kelp"
        ItemLabels.icon("SMOOTH_QUARTZ") shouldBe "quartz_block_bottom"
    }

    "nome sem traducao vira o material legivel" {
        ItemLabels.fallbackName("RAW_IRON") shouldBe "Raw iron"
    }
})
