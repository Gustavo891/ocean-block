package ocean_block.system.drift.debris

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private val ALL_FREE: (Int) -> Boolean = { true }

class DiveRulesTest : StringSpec({

    "superficie livre a frente mantem na superficie" {
        DiveRules.targetDepth(0, 3, ALL_FREE, ALL_FREE) shouldBe 0
    }

    "superficie bloqueada e um abaixo livre mergulha um bloco" {
        DiveRules.targetDepth(0, 3, { it >= 1 }, ALL_FREE) shouldBe 1
    }

    "bloqueado de 0 ate a profundidade maxima nao tem passagem" {
        DiveRules.targetDepth(0, 3, { false }, ALL_FREE).shouldBeNull()
    }

    "passagem abaixo da profundidade maxima nao conta" {
        DiveRules.targetDepth(0, 3, { it >= 4 }, ALL_FREE).shouldBeNull()
    }

    "nao atravessa bloco solido na coluna atual" {
        DiveRules.targetDepth(0, 3, { it >= 2 }, { it != 1 }).shouldBeNull()
    }

    "submerso com superficie a frente e coluna livres sobe" {
        DiveRules.targetDepth(2, 3, ALL_FREE, ALL_FREE) shouldBe 0
    }

    "submerso sob o raft nao sobe atraves dele" {
        DiveRules.targetDepth(1, 3, ALL_FREE, { it != 0 }) shouldBe 1
    }

    "submerso a frente livre so mais abaixo desce mais" {
        DiveRules.targetDepth(1, 3, { it == 3 }, ALL_FREE) shouldBe 3
    }

    "profundidade maxima zero nunca mergulha" {
        DiveRules.targetDepth(0, 0, { it >= 1 }, ALL_FREE).shouldBeNull()
    }

    "stepHeight descendo nao ultrapassa o alvo" {
        DiveRules.stepHeight(62.85, 61.5, 0.16) shouldBe (62.69 plusOrMinus 1e-9)
        DiveRules.stepHeight(61.6, 61.5, 0.16) shouldBe 61.5
    }

    "stepHeight subindo nao ultrapassa o alvo" {
        DiveRules.stepHeight(60.5, 62.85, 0.16) shouldBe (60.66 plusOrMinus 1e-9)
        DiveRules.stepHeight(62.8, 62.85, 0.16) shouldBe 62.85
    }

    "stepHeight no alvo fica parado" {
        DiveRules.stepHeight(61.5, 61.5, 0.16) shouldBe 61.5
    }

    "atTarget usa tolerancia de 0.05" {
        DiveRules.atTarget(61.54, 61.5) shouldBe true
        DiveRules.atTarget(61.46, 61.5) shouldBe true
        DiveRules.atTarget(61.56, 61.5) shouldBe false
        DiveRules.atTarget(61.44, 61.5) shouldBe false
    }

    "fadeProgress vai de 0 a 1 e e limitado" {
        DiveRules.fadeProgress(0, 20) shouldBe 0.0
        DiveRules.fadeProgress(10, 20) shouldBe (0.5 plusOrMinus 1e-12)
        DiveRules.fadeProgress(20, 20) shouldBe 1.0
        DiveRules.fadeProgress(40, 20) shouldBe 1.0
        DiveRules.fadeProgress(-4, 20) shouldBe 0.0
    }

    "fadeProgress com duracao zero termina na hora" {
        DiveRules.fadeProgress(0, 0) shouldBe 1.0
    }

    "altura do nivel na superficie desconta o afundamento" {
        DiveRules.levelY(0, 62.85, 0.12) shouldBe (62.73 plusOrMinus 1e-9)
    }

    "altura do nivel submerso fica no meio do bloco" {
        DiveRules.levelY(1, 62.85, 0.12) shouldBe 61.5
        DiveRules.levelY(3, 62.85, 0.0) shouldBe 59.5
    }

    "balanco reduzido fora da superficie" {
        DiveRules.bobScale(62.85, 62.85) shouldBe 1.0
        DiveRules.bobScale(61.5, 62.85) shouldBe (0.3 plusOrMinus 1e-12)
    }
})
