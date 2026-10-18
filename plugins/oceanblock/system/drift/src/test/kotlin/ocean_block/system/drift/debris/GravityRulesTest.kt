package ocean_block.system.drift.debris

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private val NONE: (Int) -> Boolean = { false }

class GravityRulesTest : StringSpec({

    "queda acelera a cada tick" {
        GravityRules.fall(0.0, 1) shouldBe (-0.0392 plusOrMinus 1e-12)
        GravityRules.fall(0.0, 2) shouldBe (-0.077616 plusOrMinus 1e-12)
        GravityRules.fall(0.0, 2) shouldBeLessThan GravityRules.fall(0.0, 1)
    }

    "queda respeita o limite de -1 bloco por tick" {
        GravityRules.fall(0.0, 1000) shouldBe -1.0
        GravityRules.fall(-0.99, 5) shouldBe -1.0
    }

    "queda sem ticks mantem a velocidade" {
        GravityRules.fall(-0.3, 0) shouldBe -0.3
    }

    "descida soma o deslocamento de cada tick" {
        GravityRules.descent(0.0, 2) shouldBe (-0.116816 plusOrMinus 1e-12)
        GravityRules.descent(-1.0, 2) shouldBe -2.0
    }

    "coluna com agua no nivel do raft tem superficie 0" {
        GravityRules.surfaceDepth(7, { true }, NONE) shouldBe 0
    }

    "agua so em 2 com ar acima tem superficie 2" {
        GravityRules.surfaceDepth(7, { it >= 2 }, NONE) shouldBe 2
    }

    "solido acima da agua deixa a coluna seca" {
        GravityRules.surfaceDepth(7, { it >= 2 }, { it == 1 }).shouldBeNull()
    }

    "coluna so de ar e seca" {
        GravityRules.surfaceDepth(7, NONE, NONE).shouldBeNull()
    }

    "agua alem da busca nao conta" {
        GravityRules.surfaceDepth(3, { it >= 5 }, NONE).shouldBeNull()
    }

    "sem agua e sem solido abaixo cai" {
        GravityRules.support(waterHere = false, solidBelow = false) shouldBe Support.FALL
    }

    "solido logo abaixo fica no chao" {
        GravityRules.support(waterHere = false, solidBelow = true) shouldBe Support.GROUND
    }

    "na agua boia" {
        GravityRules.support(waterHere = true, solidBelow = false) shouldBe Support.FLOAT
        GravityRules.support(waterHere = true, solidBelow = true) shouldBe Support.FLOAT
    }

    "queda pelo ar continua no ar" {
        GravityRules.settle(62.85, 62.5, NONE, NONE) shouldBe Fall.Airborne
    }

    "queda que entra na agua volta a boiar" {
        GravityRules.settle(61.2, 59.9, { it == 60 }, NONE) shouldBe Fall.Splash(60)
    }

    "queda que encosta em solido fica no topo dele" {
        GravityRules.settle(61.2, 59.9, NONE, { it == 60 }) shouldBe Fall.Landed(61)
    }

    "queda que nao chega ao bloco solido segue no ar" {
        GravityRules.settle(61.2, 61.05, NONE, { it == 60 }) shouldBe Fall.Airborne
    }

    "detrito dentro de bloco solido sobe para o topo dele" {
        GravityRules.settle(62.85, 62.8, NONE, { it == 62 }) shouldBe Fall.Landed(63)
    }

    "altura no chao fica acima do topo conforme a escala" {
        GravityRules.restY(60, 0.6) shouldBe (60.15 plusOrMinus 1e-9)
    }

    "abaixo do mundo ou muito abaixo do raft sai do mundo" {
        GravityRules.outOfWorld(-1.0, 62, 0) shouldBe true
        GravityRules.outOfWorld(-2.5, 62, -64) shouldBe true
        GravityRules.outOfWorld(0.0, 62, -64) shouldBe false
    }

    "nivel na superficie de coluna rebaixada usa a fracao da altura" {
        DiveRules.levelY(2, true, 62.85, 0.0) shouldBe (60.85 plusOrMinus 1e-9)
        DiveRules.levelY(2, false, 62.85, 0.12) shouldBe 60.5
    }
})
