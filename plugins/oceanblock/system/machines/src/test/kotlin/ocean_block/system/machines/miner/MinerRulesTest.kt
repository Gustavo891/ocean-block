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

    "broca de ferro quebra um bloco a cada 4 ciclos" {
        val gain = DrillTier.IRON.gain
        val steps = generateSequence(MinerRules.advance(0, gain)) { MinerRules.advance(it, gain) }.take(4).toList()
        steps shouldBe listOf(30, 60, 90, 120)
        steps.map(MinerRules::isDue) shouldBe listOf(false, false, false, true)
        MinerRules.afterBreak(120) shouldBe 0
    }

    "obsidiana guarda a sobra e quebra 2 blocos a cada 3 ciclos" {
        val gain = DrillTier.OBSIDIAN.gain
        MinerRules.advance(0, gain) shouldBe 80
        MinerRules.advance(80, gain) shouldBe 160
        MinerRules.isDue(160) shouldBe true
        MinerRules.afterBreak(160) shouldBe 40
        MinerRules.advance(40, gain) shouldBe 120
        MinerRules.isDue(120) shouldBe true
        MinerRules.afterBreak(120) shouldBe 0
    }

    "sem bloco para quebrar o progresso para no teto do nivel" {
        MinerRules.advance(120, DrillTier.IRON.gain) shouldBe 149
        MinerRules.advance(199, DrillTier.OBSIDIAN.gain) shouldBe 199
        MinerRules.advance(120, DrillTier.NETHERITE.gain) shouldBe 239
    }

    "um bloco custa 40 E e o armazenamento tem 27 pilhas" {
        MinerRules.BLOCK_UNITS shouldBe 120
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
