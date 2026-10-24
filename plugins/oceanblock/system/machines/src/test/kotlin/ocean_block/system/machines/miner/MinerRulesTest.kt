package ocean_block.system.machines.miner

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import ocean_block.system.machines.MachineIds
import ocean_block.system.machines.pickaxe.AutoSmelt

class MinerRulesTest : StringSpec({

    "minera pedra, pedregulho e minerios" {
        listOf("COBBLESTONE", "STONE", "COAL_ORE", "IRON_ORE", "DIAMOND_ORE", "DEEPSLATE_DIAMOND_ORE", "DEEPSLATE_EMERALD_ORE")
            .forEach { MinerRules.canMine(it) shouldBe true }
    }

    "nao minera o resto da ilha" {
        listOf("DIRT", "GRASS_BLOCK", "BEDROCK", "OAK_LOG", "NOTE_BLOCK", "AIR", "WATER")
            .forEach { MinerRules.canMine(it) shouldBe false }
    }

    "progresso satura no intervalo" {
        MinerRules.advance(0, 10) shouldBe 10
        MinerRules.advance(35, 10) shouldBe MinerRules.INTERVAL
        MinerRules.isDue(39) shouldBe false
        MinerRules.isDue(40) shouldBe true
    }

    "um bloco a cada 2 segundos custa 40 E" {
        MinerRules.INTERVAL shouldBe 40
        MinerRules.ENERGY_PER_BLOCK shouldBe 40L
        MinerRules.STORAGE shouldBe 27
    }

    "auto-fundicao troca bruto por lingote e pedregulho por pedra" {
        AutoSmelt.smelt("RAW_IRON") shouldBe "IRON_INGOT"
        AutoSmelt.smelt("RAW_COPPER") shouldBe "COPPER_INGOT"
        AutoSmelt.smelt("RAW_GOLD") shouldBe "GOLD_INGOT"
        AutoSmelt.smelt("COBBLESTONE") shouldBe "STONE"
        AutoSmelt.smelt("DIAMOND") shouldBe null
    }

    "ids das maquinas" {
        MachineIds.blocks shouldBe listOf("gerador_energia", "painel_solar", "fornalha_eletrica", "minerador")
        MachineIds.PICKAXE shouldBe "picareta_fundidora"
    }
})
