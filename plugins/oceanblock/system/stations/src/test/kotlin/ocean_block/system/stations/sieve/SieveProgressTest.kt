package ocean_block.system.stations.sieve

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class SieveProgressTest : StringSpec({

    val empty = SieveState(null, 0)

    "carregar a peneira de madeira pede 6 etapas" {
        SieveProgress.load(empty, SieveInput.GRAVEL, SieveTier.WOOD) shouldBe SieveState(SieveInput.GRAVEL, 6)
    }

    "nao carrega uma peneira ja carregada" {
        SieveProgress.load(SieveState(SieveInput.SAND, 3), SieveInput.GRAVEL, SieveTier.WOOD).shouldBeNull()
    }

    "seis etapas dao cinco avancos e terminam com o material" {
        var state = checkNotNull(SieveProgress.load(empty, SieveInput.GRAVEL, SieveTier.WOOD))
        repeat(5) {
            val step = SieveProgress.step(state)
            step.shouldBeInstanceOf<SieveStep.Advanced>()
            state = step.state
        }
        SieveProgress.step(state) shouldBe SieveStep.Done(SieveInput.GRAVEL)
    }

    "etapa numa peneira vazia nao faz nada" {
        SieveProgress.step(empty) shouldBe SieveStep.Empty
    }

    "nivel do compostor acompanha a camada" {
        SieveProgress.composterLevel(empty, SieveTier.WOOD) shouldBe 0
        SieveProgress.composterLevel(SieveState(SieveInput.DIRT, 6), SieveTier.WOOD) shouldBe 6
        SieveProgress.composterLevel(SieveState(SieveInput.DIRT, 1), SieveTier.WOOD) shouldBeGreaterThanOrEqual 1
    }

    "duas etapas no mesmo tick contam uma" {
        SieveProgress.allowed(null, 0) shouldBe true
        SieveProgress.allowed(10, 10) shouldBe false
        SieveProgress.allowed(10, 12) shouldBe false
        SieveProgress.allowed(10, 14) shouldBe true
    }

    "estado vai e volta pelos contadores do bloco" {
        val state = SieveState(SieveInput.SAND, 4)
        SieveProgress.fromCounters(SieveProgress.toCounters(state)) shouldBe state
        SieveProgress.fromCounters(emptyMap()) shouldBe empty
    }
})
