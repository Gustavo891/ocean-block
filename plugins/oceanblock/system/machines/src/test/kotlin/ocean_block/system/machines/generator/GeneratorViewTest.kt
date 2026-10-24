package ocean_block.system.machines.generator

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import ocean_block.system.machines.energy.EnergyMeter
import java.util.UUID

class GeneratorViewTest : StringSpec({

    val fuel: (String) -> Int? = { mapOf("COAL" to 1600, "OAK_PLANKS" to 300)[it] }

    "barra divide cheio e vazio pela energia" {
        GeneratorView.bar(5_000, 10_000, 20) shouldBe ("|".repeat(10) to "|".repeat(10))
        GeneratorView.bar(0, 10_000, 20) shouldBe ("" to "|".repeat(20))
        GeneratorView.bar(10_000, 10_000, 20) shouldBe ("|".repeat(20) to "")
    }

    "barra com pouca energia ainda mostra um traco" {
        GeneratorView.bar(1, 10_000, 20).first shouldBe "|"
    }

    "combustiveis do inventario somam por tipo e ignoram o resto" {
        val entries = GeneratorView.fuelEntries(listOf("COAL" to 10, "DIRT" to 64, "COAL" to 5, "OAK_PLANKS" to 3), fuel)
        entries shouldBe listOf(FuelStack("COAL", 15), FuelStack("OAK_PLANKS", 3))
    }

    "queima restante em segundos conta o combustivel guardado" {
        GeneratorView.burnSeconds(GeneratorState("COAL", 2, 400, 0), fuel) shouldBe (400 + 2 * 1600) / 20
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
})
