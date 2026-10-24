package ocean_block.system.machines.generator

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import ocean_block.system.machines.energy.EnergyMeter
import java.util.UUID

class GeneratorViewTest : StringSpec({

    val fuel: (String) -> Int? = { mapOf("COAL" to 1600, "OAK_PLANKS" to 300)[it] }

    "queima restante em segundos conta todos os slots" {
        val state = GeneratorState(listOf(FuelStack("COAL", 2), null, FuelStack("OAK_PLANKS", 4)) + List(6) { null }, 400, 0)
        GeneratorView.burnSeconds(state, fuel) shouldBe (400 + 2 * 1600 + 4 * 300) / 20
    }

    "bateria acende um gomo a cada 10 por cento e qualquer energia mostra o primeiro" {
        GeneratorView.batteryLevel(0, 10_000) shouldBe 0
        GeneratorView.batteryLevel(1, 10_000) shouldBe 1
        GeneratorView.batteryLevel(1_000, 10_000) shouldBe 1
        GeneratorView.batteryLevel(6_300, 10_000) shouldBe 7
        GeneratorView.batteryLevel(10_000, 10_000) shouldBe 10
        GeneratorView.batteryLevel(20_000, 10_000) shouldBe 10
    }

    "porcentagem da bateria arredonda para baixo" {
        GeneratorView.percent(6_399, 10_000) shouldBe 63
        GeneratorView.percent(10_000, 10_000) shouldBe 100
    }

    "medidor soma o gasto e fecha a media a cada segundo" {
        val meter = EnergyMeter()
        val id = UUID(0, 1)
        meter.record(id, 10, tick = 0)
        meter.record(id, 10, tick = 10)
        meter.perSecond(id, tick = 19) shouldBe 0
        meter.perSecond(id, tick = 20) shouldBe 20
        meter.perSecond(id, tick = 45) shouldBe 0
    }

    "medidor lista as maquinas que puxaram energia ha pouco" {
        val meter = EnergyMeter()
        val source = UUID(0, 1)
        val furnace = UUID(0, 2)
        val miner = UUID(0, 3)
        meter.record(source, 10, tick = 0, consumer = furnace)
        meter.record(source, 10, tick = 30, consumer = miner)
        meter.consumers(source, tick = 35) shouldBe listOf(furnace, miner)
        meter.consumers(source, tick = 60) shouldBe listOf(miner)
        meter.consumers(source, tick = 100) shouldBe emptyList()
    }
})
