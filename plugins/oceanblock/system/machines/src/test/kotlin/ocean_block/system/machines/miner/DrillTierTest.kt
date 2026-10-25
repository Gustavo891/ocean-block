package ocean_block.system.machines.miner

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class DrillTierTest : StringSpec({

    "sem counter ou com nivel invalido a broca e de ferro" {
        DrillTier.of(null) shouldBe DrillTier.IRON
        DrillTier.of(-1) shouldBe DrillTier.IRON
        DrillTier.of(9) shouldBe DrillTier.IRON
        DrillTier.of(2) shouldBe DrillTier.DIAMOND
    }

    "ordem e tempo por bloco dos niveis" {
        DrillTier.entries.map { it.level } shouldBe listOf(0, 1, 2, 3, 4)
        DrillTier.entries.map { it.ticksPerBlock } shouldBe listOf(40, 30, 20, 15, 10)
    }

    "ganho de progresso por ciclo de 10 ticks" {
        DrillTier.entries.map { it.gain } shouldBe listOf(30, 40, 60, 80, 120)
    }

    "cada bloco custa cerca de 40 E, cobrados por ciclo" {
        DrillTier.entries.map { it.cycleEnergy } shouldBe listOf(10L, 14L, 20L, 27L, 40L)
    }

    "textos de tempo por bloco e gasto por segundo" {
        DrillTier.entries.map { it.secondsLabel } shouldBe listOf("2", "1,5", "1", "0,75", "0,5")
        DrillTier.entries.map { it.energyPerSecond } shouldBe listOf(20L, 28L, 40L, 54L, 80L)
    }

    "so as brocas acima de ferro tem item" {
        DrillTier.IRON.itemId shouldBe null
        DrillTier.entries.drop(1).map { it.itemId } shouldBe
            listOf("broca_ouro", "broca_diamante", "broca_obsidiana", "broca_netherite")
        DrillTier.byItemId("broca_ouro") shouldBe DrillTier.GOLD
        DrillTier.byItemId("broca_netherite") shouldBe DrillTier.NETHERITE
        DrillTier.byItemId(null) shouldBe null
        DrillTier.byItemId("minerador") shouldBe null
    }

    "modelo da broca que gira por nivel" {
        DrillTier.entries.map { it.model } shouldBe listOf(
            "minerador_broca",
            "minerador_broca_ouro",
            "minerador_broca_diamante",
            "minerador_broca_obsidiana",
            "minerador_broca_netherite",
        )
    }

    "o counter do nivel" {
        DrillTier.KEY shouldBe "minerador_broca"
    }
})
