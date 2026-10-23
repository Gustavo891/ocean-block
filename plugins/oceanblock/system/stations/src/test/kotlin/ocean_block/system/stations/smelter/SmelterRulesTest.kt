package ocean_block.system.stations.smelter

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.random.Random

private object AlwaysHits : Random() {
    override fun nextBits(bitCount: Int): Int = 0
}

private object NeverHits : Random() {
    override fun nextBits(bitCount: Int): Int = (1 shl bitCount) - 1
}

private fun loaded(id: String, amount: Int, fuel: String, fuelAmount: Int): SmelterState {
    val withOre = SmelterRules.insertOre(SmelterState.EMPTY, id, amount).first
    return SmelterRules.insertFuel(withOre, fuel, fuelAmount).first
}

class SmelterRulesTest : StringSpec({

    "um minerio bruto e uma tabua fazem um lingote em 10 segundos" {
        val state = SmelterRules.tick(loaded("RAW_IRON", 8, "OAK_PLANKS", 1), SmelterTier.BASIC, 200, NeverHits)

        state.output shouldBe "IRON_INGOT"
        state.outputCount shouldBe 1
        state.inputCount shouldBe 7
        state.burnTicks shouldBe 100
    }

    "sem combustivel o progresso para" {
        val first = SmelterRules.tick(loaded("RAW_IRON", 8, "OAK_PLANKS", 1), SmelterTier.BASIC, 200, NeverHits)
        val burnedOut = SmelterRules.tick(first, SmelterTier.BASIC, 100, NeverHits)
        val idle = SmelterRules.tick(burnedOut, SmelterTier.BASIC, 400, NeverHits)

        burnedOut.burnTicks shouldBe 0
        burnedOut.progressTicks shouldBe 100
        idle shouldBe burnedOut
    }

    "um tipo de minerio por vez e no maximo 32 na entrada" {
        val iron = SmelterRules.insertOre(SmelterState.EMPTY, "RAW_IRON", 40)

        iron.second shouldBe 32
        SmelterRules.insertOre(iron.first, "RAW_COPPER", 4).second shouldBe 0
        SmelterRules.insertOre(SmelterState.EMPTY, "DIRT", 4).second shouldBe 0
    }

    "combustivel guarda no maximo 16 de um tipo" {
        val coal = SmelterRules.insertFuel(SmelterState.EMPTY, "COAL", 20)

        coal.second shouldBe 16
        SmelterRules.insertFuel(coal.first, "OAK_PLANKS", 1).second shouldBe 0
        SmelterRules.insertFuel(SmelterState.EMPTY, "DIRT", 1).second shouldBe 0
    }

    "minerio bruto vanilla rende um lingote por unidade" {
        val state = SmelterRules.tick(loaded("RAW_COPPER", 1, "COAL", 1), SmelterTier.BASIC, 200, NeverHits)

        state.output shouldBe "COPPER_INGOT"
        state.outputCount shouldBe 1
        state.input shouldBe null
    }

    "fundicao melhorada leva metade do tempo e pode dobrar" {
        val state = SmelterRules.tick(loaded("RAW_GOLD", 1, "COAL", 1), SmelterTier.IMPROVED, 100, AlwaysHits)

        state.output shouldBe "GOLD_INGOT"
        state.outputCount shouldBe 2
    }

    "saida cheia nao avanca" {
        val full = loaded("RAW_IRON", 8, "COAL", 4).copy(output = "IRON_INGOT", outputCount = 16)

        SmelterRules.tick(full, SmelterTier.BASIC, 400, NeverHits) shouldBe full
    }

    "retirar devolve lingotes; retirar tudo devolve entrada e combustivel" {
        val state = SmelterRules.tick(loaded("RAW_IRON", 8, "OAK_PLANKS", 3), SmelterTier.BASIC, 200, NeverHits)

        val (afterTake, taken) = SmelterRules.takeOutput(state)
        taken shouldBe ("IRON_INGOT" to 1)
        afterTake.outputCount shouldBe 0

        val (empty, all) = SmelterRules.takeAll(state)
        all shouldBe listOf("IRON_INGOT" to 1, "RAW_IRON" to 7, "OAK_PLANKS" to 2)
        empty shouldBe SmelterState.EMPTY
    }

    "estado vai e volta pelos contadores do bloco" {
        val state = SmelterRules.tick(loaded("RAW_COPPER", 9, "DRIED_KELP_BLOCK", 2), SmelterTier.BASIC, 250, NeverHits)

        SmelterState.fromCounters(state.toCounters()) shouldBe state
        SmelterState.fromCounters(emptyMap()) shouldBe SmelterState.EMPTY
    }

    "so aceita minerio bruto vanilla, sem os antigos pedacos" {
        SmelterRules.accepts("RAW_IRON") shouldBe true
        SmelterRules.accepts("pedaco_ferro") shouldBe false
    }

    "combustiveis e tempos da spec" {
        Fuel.ticks("OAK_PLANKS") shouldBe 300
        Fuel.ticks("SPRUCE_LOG") shouldBe 1200
        Fuel.ticks("STICK") shouldBe 100
        Fuel.ticks("CHARCOAL") shouldBe 1600
        Fuel.ticks("DRIED_KELP_BLOCK") shouldBe 4000
        Fuel.ticks("SAND") shouldBe null
    }
})
