package ocean_block.system.machines.miner

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MinerCrackRulesTest : StringSpec({

    "avanca entre dois ciclos pagos no ritmo da broca sem passar do proximo" {
        MinerCrackRules.progress(paid = 30, gain = 30, sinceTicks = 0) shouldBe 30
        MinerCrackRules.progress(paid = 30, gain = 30, sinceTicks = 5) shouldBe 45
        MinerCrackRules.progress(paid = 30, gain = 30, sinceTicks = 25) shouldBe 60
    }

    "nunca passa de um bloco" {
        MinerCrackRules.progress(paid = 90, gain = 30, sinceTicks = 25) shouldBe MinerRules.BLOCK_UNITS
        MinerCrackRules.progress(paid = 80, gain = 40, sinceTicks = 10) shouldBe MinerRules.BLOCK_UNITS
    }

    "estagio vai de 0 a 9 pelo progresso" {
        MinerCrackRules.stage(0) shouldBe 0
        MinerCrackRules.stage(12) shouldBe 1
        MinerCrackRules.stage(60) shouldBe 5
        MinerCrackRules.stage(119) shouldBe 9
        MinerCrackRules.stage(MinerRules.BLOCK_UNITS) shouldBe 9
    }

    "dano mandado ao cliente sobe com o estagio e nunca e zero com rachadura" {
        MinerCrackRules.damage(0) shouldBe 0.1f
        MinerCrackRules.damage(9) shouldBe 1f
    }
})
