package ocean_block.system.machines.generator

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class GeneratorRulesTest : StringSpec({

    val fuel: (String) -> Int? = { mapOf("COAL" to 1600, "STICK" to 100, "OAK_LOG" to 1200)[it] }

    fun state(vararg stacks: FuelStack?, burn: Int = 0, energy: Long = 0): GeneratorState =
        GeneratorState(List(GeneratorState.SLOTS) { stacks.getOrNull(it) }, burn, energy)

    "carvao em 10 ticks gera 20 E e gasta 1 carvao" {
        val (loaded, _) = GeneratorRules.insertFuel(GeneratorState.EMPTY, "COAL", 2, fuel)
        GeneratorRules.tick(loaded, 10, fuel) shouldBe state(FuelStack("COAL", 1), burn = 1590, energy = 20)
    }

    "bateria cheia nao acende nem gasta" {
        val full = state(FuelStack("COAL", 3), energy = GeneratorRules.CAPACITY)
        GeneratorRules.tick(full, 10, fuel) shouldBe full
    }

    "para no teto de 10000" {
        val almost = state(FuelStack("COAL", 1), burn = 500, energy = GeneratorRules.CAPACITY - 5)
        val next = GeneratorRules.tick(almost, 10, fuel)
        next.energy shouldBe GeneratorRules.CAPACITY
        next.burnTicks shouldBe 497
    }

    "acende o primeiro slot com combustivel e esvazia o slot no ultimo" {
        val current = state(null, FuelStack("STICK", 1), FuelStack("COAL", 2), burn = 4)
        GeneratorRules.tick(current, 10, fuel) shouldBe state(null, null, FuelStack("COAL", 2), burn = 94, energy = 20)
    }

    "sem combustivel nao gera" {
        GeneratorRules.tick(GeneratorState.EMPTY, 10, fuel) shouldBe GeneratorState.EMPTY
    }

    "combustivel diferente vai para o proximo slot livre" {
        val current = state(FuelStack("COAL", 4))
        GeneratorRules.insertFuel(current, "STICK", 4, fuel) shouldBe (state(FuelStack("COAL", 4), FuelStack("STICK", 4)) to 4)
    }

    "completa a pilha do mesmo tipo antes de abrir outro slot" {
        val current = state(FuelStack("COAL", 60), null, FuelStack("COAL", 10))
        GeneratorRules.insertFuel(current, "COAL", 70, fuel) shouldBe
            (state(FuelStack("COAL", 64), FuelStack("COAL", 12), FuelStack("COAL", 64)) to 70)
    }

    "nao combustivel e recusado" {
        GeneratorRules.insertFuel(GeneratorState.EMPTY, "DIRT", 4, fuel) shouldBe (GeneratorState.EMPTY to 0)
    }

    "guarda no maximo uma pilha de 64 por slot" {
        val full = GeneratorState(List(GeneratorState.SLOTS) { FuelStack("COAL", 64) }, 0, 0)
        GeneratorRules.insertFuel(full, "COAL", 10, fuel) shouldBe (full to 0)
        GeneratorRules.insertFuel(full, "STICK", 10, fuel) shouldBe (full to 0)
    }

    "retirar devolve tudo e mantem a queima" {
        val current = state(FuelStack("COAL", 5), null, FuelStack("STICK", 3), burn = 100, energy = 30)
        GeneratorRules.takeFuel(current) shouldBe (state(burn = 100, energy = 30) to listOf(FuelStack("COAL", 5), FuelStack("STICK", 3)))
        GeneratorRules.takeFuel(GeneratorState.EMPTY) shouldBe (GeneratorState.EMPTY to emptyList())
    }

    "retirar um slot devolve so aquela pilha" {
        val current = state(FuelStack("COAL", 5), null, FuelStack("STICK", 3), burn = 100)
        GeneratorRules.takeSlot(current, 2) shouldBe (state(FuelStack("COAL", 5), burn = 100) to FuelStack("STICK", 3))
        GeneratorRules.takeSlot(current, 1) shouldBe (current to null)
        GeneratorRules.takeSlot(current, 20) shouldBe (current to null)
    }

    "ida e volta pelos counters" {
        val current = state(FuelStack("OAK_LOG", 7), null, FuelStack("COAL", 64), burn = 321, energy = 4567)
        GeneratorState.fromCounters(current.toCounters()) shouldBe current
        GeneratorState.fromCounters(emptyMap()) shouldBe GeneratorState.EMPTY
    }

    "combustivel antigo de um tipo vira o primeiro slot" {
        GeneratorState.fromCounters(mapOf("gerador_fuel/COAL" to 12L, "gerador_burn" to 5L)) shouldBe state(FuelStack("COAL", 12), burn = 5)
    }
})
