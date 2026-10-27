package ocean_block.system.machines.generator

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class FuelTest : StringSpec({

    "recursos do mar queimam no tempo combinado" {
        Fuel.ticks("DRIED_KELP") shouldBe 400
        Fuel.ticks("DRIED_KELP_BLOCK") shouldBe 4000
        Fuel.ticks("PRISMARINE_SHARD") shouldBe 1600
    }

    "o bloco de alga seca rende mais que as nove algas soltas" {
        val loose = (Fuel.ticks("DRIED_KELP") ?: 0) * 9
        (loose < (Fuel.ticks("DRIED_KELP_BLOCK") ?: 0)) shouldBe true
    }

    "combustiveis vanilla e outros recursos nao queimam mais" {
        listOf("COAL", "CHARCOAL", "STICK", "OAK_PLANKS", "OAK_LOG", "BIRCH_WOOD", "KELP", "PRISMARINE_CRYSTALS").forEach {
            Fuel.ticks(it).shouldBeNull()
        }
    }
})
