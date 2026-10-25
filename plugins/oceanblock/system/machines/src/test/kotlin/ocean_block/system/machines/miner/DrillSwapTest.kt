package ocean_block.system.machines.miner

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class DrillSwapTest : StringSpec({

    "encaixar sobre a de ferro nao devolve nada" {
        DrillSwap.mount(DrillTier.IRON, DrillTier.GOLD) shouldBe DrillSwap.Result(DrillTier.GOLD, null)
    }

    "encaixar sobre outra broca devolve a anterior" {
        DrillSwap.mount(DrillTier.GOLD, DrillTier.DIAMOND) shouldBe DrillSwap.Result(DrillTier.DIAMOND, DrillTier.GOLD)
    }

    "encaixar a mesma broca nao muda nada" {
        DrillSwap.mount(DrillTier.GOLD, DrillTier.GOLD) shouldBe null
    }

    "tirar a broca volta para ferro e devolve a broca" {
        DrillSwap.unmount(DrillTier.OBSIDIAN) shouldBe DrillSwap.Result(DrillTier.IRON, DrillTier.OBSIDIAN)
    }

    "a de ferro nao sai" {
        DrillSwap.unmount(DrillTier.IRON) shouldBe null
    }
})
