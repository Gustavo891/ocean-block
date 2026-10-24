package ocean_block.system.machines.miner

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MinerCrackRulesTest : StringSpec({

    "avanca entre dois ciclos pagos sem passar do proximo" {
        MinerCrackRules.progress(paid = 10, sinceTicks = 0) shouldBe 10
        MinerCrackRules.progress(paid = 10, sinceTicks = 4) shouldBe 14
        MinerCrackRules.progress(paid = 10, sinceTicks = 25) shouldBe 20
    }

    "nunca passa do intervalo do bloco" {
        MinerCrackRules.progress(paid = 36, sinceTicks = 8) shouldBe MinerRules.INTERVAL
    }

    "estagio vai de 0 a 9 pelo progresso" {
        MinerCrackRules.stage(0) shouldBe 0
        MinerCrackRules.stage(4) shouldBe 1
        MinerCrackRules.stage(20) shouldBe 5
        MinerCrackRules.stage(39) shouldBe 9
        MinerCrackRules.stage(MinerRules.INTERVAL) shouldBe 9
    }

    "dano mandado ao cliente sobe com o estagio e nunca e zero com rachadura" {
        MinerCrackRules.damage(0) shouldBe 0.1f
        MinerCrackRules.damage(9) shouldBe 1f
    }
})
