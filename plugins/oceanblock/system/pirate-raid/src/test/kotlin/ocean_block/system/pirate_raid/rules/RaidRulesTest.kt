package ocean_block.system.pirate_raid.rules

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import ocean_block.system.pirate_raid.config.RaidConfig
import kotlin.random.Random

class RaidRulesTest : FunSpec({

    context("RaidRoll") {
        test("elegível sem raid ativa, outro dia e nível suficiente") {
            RaidRoll.eligible(active = false, lastDay = "2026-10-26", today = "2026-10-27", maxLevel = 10, minLevel = 10) shouldBe true
            RaidRoll.eligible(active = false, lastDay = null, today = "2026-10-27", maxLevel = 30, minLevel = 10) shouldBe true
        }

        test("não elegível com raid ativa, no mesmo dia ou abaixo do nível") {
            RaidRoll.eligible(active = true, lastDay = null, today = "2026-10-27", maxLevel = 30, minLevel = 10) shouldBe false
            RaidRoll.eligible(active = false, lastDay = "2026-10-27", today = "2026-10-27", maxLevel = 30, minLevel = 10) shouldBe false
            RaidRoll.eligible(active = false, lastDay = null, today = "2026-10-27", maxLevel = 9, minLevel = 10) shouldBe false
        }

        test("acerta só abaixo da chance") {
            RaidRoll.hits(0.02, 0.03) shouldBe true
            RaidRoll.hits(0.03, 0.03) shouldBe false
            RaidRoll.hits(0.0, 0.0) shouldBe false
        }
    }

    context("WaveTable") {
        val bands = RaidConfig.DEFAULT.bands

        test("usa a maior faixa até o nível") {
            WaveTable.band(bands, 10).minLevel shouldBe 10
            WaveTable.band(bands, 25).minLevel shouldBe 20
            WaveTable.band(bands, 50).minLevel shouldBe 40
            WaveTable.band(bands, 3).minLevel shouldBe 10
        }

        test("Super Pirata só na última onda") {
            val band = WaveTable.band(bands, 40)
            WaveTable.crew(band, 1, Random(1)).superPirate shouldBe false
            WaveTable.crew(band, 2, Random(1)).superPirate shouldBe false
            WaveTable.crew(band, 3, Random(1)).superPirate shouldBe true
        }

        test("brutamontes arredondado pela fração") {
            val band = WaveTable.band(bands, 40)
            val crew = WaveTable.crew(band, 3, Random(1))
            crew.brutes shouldBe 5
            crew.pillagers shouldBe 5
            val first = WaveTable.crew(WaveTable.band(bands, 10), 1, Random(1))
            first.brutes shouldBe 0
            first.pillagers shouldBe 3
        }

        test("barcos levam dois piratas e o Super Pirata vai sozinho") {
            WaveTable.boats(Crew(pillagers = 3, brutes = 0, superPirate = false)) shouldBe 2
            WaveTable.boats(Crew(pillagers = 5, brutes = 5, superPirate = true)) shouldBe 6
        }
    }

    context("LootRoller") {
        val entries = listOf(
            LootEntry("moeda", 1.0, 1, 1),
            LootEntry("DIAMOND", 1.0, 2, 2),
            LootEntry("GOLDEN_APPLE", 0.0, 1, 1),
        )

        test("chance 1 sempre e 0 nunca") {
            val drops = LootRoller.roll(entries, 1, Random(7))
            drops shouldBe listOf(Drop("moeda", 1), Drop("DIAMOND", 2))
        }

        test("moeda não se repete com várias rolagens") {
            val drops = LootRoller.roll(entries, 3, Random(7))
            drops.count { it.item == "moeda" } shouldBe 1
            drops.count { it.item == "DIAMOND" } shouldBe 3
        }

        test("quantidade fica entre mínimo e máximo") {
            repeat(50) { seed ->
                val drop = LootRoller.roll(listOf(LootEntry("IRON_INGOT", 1.0, 1, 3)), 1, Random(seed)).single()
                drop.amount shouldBeInRange 1..3
            }
        }
    }

    context("CoinValue") {
        test("fator de nível") {
            CoinValue.factor(10, 0.05) shouldBe (1.0 plusOrMinus 1e-9)
            CoinValue.factor(50, 0.05) shouldBe (3.0 plusOrMinus 1e-9)
            CoinValue.factor(5, 0.05) shouldBe (1.0 plusOrMinus 1e-9)
        }

        test("moeda dentro do intervalo escalado") {
            repeat(50) { seed ->
                CoinValue.coin(150, 400, 50, 0.05, Random(seed)) shouldBeInRange 450..1200
            }
        }

        test("bônus de vitória") {
            CoinValue.victory(3000, 30, 0.05) shouldBe 6000
        }
    }

    context("BoatPath") {
        val center = Vec(0.0, 0.0)

        test("ponto no círculo") {
            val point = BoatPath.spawnPoint(center, 32.0, 0.0)
            point.x shouldBe (32.0 plusOrMinus 1e-9)
            point.z shouldBe (0.0 plusOrMinus 1e-9)
        }

        test("passo aponta para o centro com a velocidade") {
            val step = BoatPath.step(Vec(10.0, 0.0), center, 0.25)
            step.x shouldBe (-0.25 plusOrMinus 1e-9)
            step.z shouldBe (0.0 plusOrMinus 1e-9)
        }

        test("passo não ultrapassa o centro") {
            val step = BoatPath.step(Vec(0.1, 0.0), center, 0.25)
            step.x shouldBe (-0.1 plusOrMinus 1e-9)
        }

        test("desembarca perto, bloqueado ou parado") {
            BoatPath.landed(7.0, 8.0, false, 0) shouldBe true
            BoatPath.landed(20.0, 8.0, true, 0) shouldBe true
            BoatPath.landed(20.0, 8.0, false, 60) shouldBe true
            BoatPath.landed(20.0, 8.0, false, 59) shouldBe false
        }
    }

    context("RaidState") {
        fun next(wave: Int, alive: Int, elapsed: Long = 0, empty: Long = 0) =
            RaidState.next(wave, 3, alive, elapsed, 6000, empty, 600)

        test("onda limpa avança") {
            next(1, 0) shouldBe RaidPhase.Wave(2)
            next(2, 0) shouldBe RaidPhase.Wave(3)
        }

        test("última onda limpa vence") {
            next(3, 0) shouldBe RaidPhase.Won
        }

        test("vitória vale mesmo no último segundo") {
            next(3, 0, elapsed = 6000) shouldBe RaidPhase.Won
        }

        test("tempo esgotado com piratas vivos foge") {
            next(2, 3, elapsed = 6000) shouldBe RaidPhase.Fled
        }

        test("ilha vazia abandona") {
            next(2, 3, empty = 600) shouldBe RaidPhase.Abandoned
        }

        test("segue na onda enquanto há piratas e tempo") {
            next(2, 3, elapsed = 100, empty = 599) shouldBe RaidPhase.Wave(2)
        }
    }
})
