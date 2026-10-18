package one_block.system.custom_items.content.vacuum_hopper

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class VacuumHopperUpgradeTest : StringSpec({

    "raio sobe 2 por nível" {
        VacuumHopperUpgrade.radiusFor(0) shouldBe 4.0
        VacuumHopperUpgrade.radiusFor(1) shouldBe 6.0
        VacuumHopperUpgrade.radiusFor(2) shouldBe 8.0
        VacuumHopperUpgrade.radiusFor(3) shouldBe 10.0
    }

    "armazenamento no nível máximo bate exatamente o teto do inventário Bukkit" {
        VacuumHopperUpgrade.storageSlotsFor(0) shouldBe 27
        VacuumHopperUpgrade.storageSlotsFor(1) shouldBe 36
        VacuumHopperUpgrade.storageSlotsFor(2) shouldBe 45
        VacuumHopperUpgrade.storageSlotsFor(3) shouldBe 54
    }

    "velocidade desce 5 ticks por nível" {
        VacuumHopperUpgrade.tickIntervalFor(0) shouldBe 20L
        VacuumHopperUpgrade.tickIntervalFor(3) shouldBe 5L
    }

    "nível fora da faixa satura no máximo" {
        VacuumHopperUpgrade.radiusFor(99) shouldBe 10.0
        VacuumHopperUpgrade.storageSlotsFor(99) shouldBe 54
        VacuumHopperUpgrade.tickIntervalFor(99) shouldBe 5L
        VacuumHopperUpgrade.radiusFor(-1) shouldBe 4.0
    }

    "custo de cada upgrade segue a tabela do design" {
        VacuumHopperUpgrade.Radius.costFor(0) shouldBe 5_000.0
        VacuumHopperUpgrade.Radius.costFor(2) shouldBe 40_000.0
        VacuumHopperUpgrade.Storage.costFor(0) shouldBe 7_500.0
        VacuumHopperUpgrade.Speed.costFor(2) shouldBe 60_000.0
    }

    "custo de nível já no máximo é zero" {
        VacuumHopperUpgrade.Radius.costFor(3) shouldBe 0.0
        VacuumHopperUpgrade.Speed.costFor(99) shouldBe 0.0
    }

    "statLine mostra o valor resultante do nível" {
        VacuumHopperUpgrade.Radius.statLine(1) shouldBe "<yellow>6 blocos"
        VacuumHopperUpgrade.Storage.statLine(2) shouldBe "<yellow>45 slots"
        VacuumHopperUpgrade.Speed.statLine(3) shouldBe "<yellow>5 ticks"
    }
})
