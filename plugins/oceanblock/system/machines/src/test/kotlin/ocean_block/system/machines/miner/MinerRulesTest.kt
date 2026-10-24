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

    "minerador antigo em note block e adotado como barrier, o resto nao" {
        MinerRules.adopts("NOTE_BLOCK") shouldBe true
        MinerRules.adopts("AIR") shouldBe false
        MinerRules.adopts("STONE") shouldBe false
    }

    "golpes para quebrar valem dentro da janela" {
        MinerRules.hits(previous = null, now = 100) shouldBe 1
        MinerRules.hits(previous = MinerHits(2, 90), now = 100) shouldBe 3
        MinerRules.hits(previous = MinerHits(2, 50), now = 100) shouldBe 1
        MinerRules.HITS_TO_BREAK shouldBe 3
    }

    "ligado gasta a energia de um bloco a cada 2 segundos, espalhada pelos ciclos" {
        MinerRules.cycleEnergy(10) shouldBe 10L
        MinerRules.cycleEnergy(40) shouldBe MinerRules.ENERGY_PER_BLOCK
    }

    "sem counter o minerador vem ligado" {
        MinerRules.isOn(null) shouldBe true
        MinerRules.isOn(1) shouldBe true
        MinerRules.isOn(0) shouldBe false
    }

    "giro anda 90 graus por ciclo e da a volta" {
        MinerRules.nextSpin(0f) shouldBe 90f
        MinerRules.nextSpin(270f) shouldBe 0f
    }

    "ids das maquinas" {
        MachineIds.blocks shouldBe listOf("gerador_energia", "painel_solar", "fornalha_eletrica", "minerador")
        MachineIds.PICKAXE shouldBe "picareta_fundidora"
    }
})
