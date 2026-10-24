package ocean_block.system.machines.generator

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class GeneratorRulesTest : StringSpec({

    val fuel: (String) -> Int? = { mapOf("COAL" to 1600, "STICK" to 100)[it] }

    "carvao em 10 ticks gera 20 E e gasta 1 carvao" {
        val (loaded, _) = GeneratorRules.insertFuel(GeneratorState.EMPTY, "COAL", 2, fuel)
        val next = GeneratorRules.tick(loaded, 10, fuel)
        next shouldBe GeneratorState("COAL", 1, 1590, 20)
    }

    "bateria cheia nao acende nem gasta" {
        val full = GeneratorState("COAL", 3, 0, GeneratorRules.CAPACITY)
        GeneratorRules.tick(full, 10, fuel) shouldBe full
    }

    "para no teto de 10000" {
        val almost = GeneratorState("COAL", 1, 500, GeneratorRules.CAPACITY - 5)
        val next = GeneratorRules.tick(almost, 10, fuel)
        next.energy shouldBe GeneratorRules.CAPACITY
        next.burnTicks shouldBe 497
    }

    "acende o proximo combustivel quando a queima acaba" {
        val state = GeneratorState("STICK", 1, 4, 0)
        GeneratorRules.tick(state, 10, fuel) shouldBe GeneratorState(null, 0, 94, 20)
    }

    "sem combustivel nao gera" {
        GeneratorRules.tick(GeneratorState.EMPTY, 10, fuel) shouldBe GeneratorState.EMPTY
    }

    "combustivel diferente e recusado" {
        val state = GeneratorState("COAL", 4, 0, 0)
        GeneratorRules.insertFuel(state, "STICK", 4, fuel) shouldBe (state to 0)
    }

    "nao combustivel e recusado" {
        GeneratorRules.insertFuel(GeneratorState.EMPTY, "DIRT", 4, fuel) shouldBe (GeneratorState.EMPTY to 0)
    }

    "guarda no maximo 64" {
        val state = GeneratorState("COAL", 60, 0, 0)
        GeneratorRules.insertFuel(state, "COAL", 10, fuel) shouldBe (GeneratorState("COAL", 64, 0, 0) to 4)
    }

    "retirar devolve o combustivel e mantem a queima" {
        val state = GeneratorState("COAL", 5, 100, 30)
        GeneratorRules.takeFuel(state) shouldBe (GeneratorState(null, 0, 100, 30) to ("COAL" to 5))
        GeneratorRules.takeFuel(GeneratorState.EMPTY) shouldBe (GeneratorState.EMPTY to null)
    }

    "ida e volta pelos counters" {
        val state = GeneratorState("OAK_LOG", 7, 321, 4567)
        GeneratorState.fromCounters(state.toCounters()) shouldBe state
        GeneratorState.fromCounters(emptyMap()) shouldBe GeneratorState.EMPTY
    }
})
