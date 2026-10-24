package ocean_block.system.machines

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class LegacyItemsTest : StringSpec({

    "item de maquina com material antigo precisa ser trocado" {
        LegacyItems.isOutdated(MachineIds.SOLAR, itemMaterial = "NOTE_BLOCK", blockMaterial = "DAYLIGHT_DETECTOR") shouldBe true
        LegacyItems.isOutdated(MachineIds.FURNACE, itemMaterial = "NOTE_BLOCK", blockMaterial = "BLAST_FURNACE") shouldBe true
        LegacyItems.isOutdated(MachineIds.MINER, itemMaterial = "NOTE_BLOCK", blockMaterial = "BARRIER") shouldBe true
    }

    "item atual ou que nao e maquina fica como esta" {
        LegacyItems.isOutdated(MachineIds.SOLAR, itemMaterial = "DAYLIGHT_DETECTOR", blockMaterial = "DAYLIGHT_DETECTOR") shouldBe false
        LegacyItems.isOutdated(MachineIds.GENERATOR, itemMaterial = "NOTE_BLOCK", blockMaterial = "NOTE_BLOCK") shouldBe false
        LegacyItems.isOutdated("rede", itemMaterial = "NOTE_BLOCK", blockMaterial = "LOOM") shouldBe false
    }
})
