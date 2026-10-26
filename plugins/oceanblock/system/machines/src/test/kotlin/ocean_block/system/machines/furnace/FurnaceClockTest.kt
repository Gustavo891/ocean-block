package ocean_block.system.machines.furnace

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class FurnaceClockTest : StringSpec({

    "proximo item arredonda para cima o que falta" {
        FurnaceClock.nextSeconds(progress = 0, ticksPerItem = 50) shouldBe 3
        FurnaceClock.nextSeconds(progress = 30, ticksPerItem = 50) shouldBe 1
        FurnaceClock.nextSeconds(progress = 50, ticksPerItem = 50) shouldBe 0
    }

    "fila inteira soma o atual e os que faltam" {
        FurnaceClock.totalSeconds(progress = 30, queued = 3, ticksPerItem = 50) shouldBe 1 + 5
        FurnaceClock.totalSeconds(progress = 0, queued = 0, ticksPerItem = 50) shouldBe 0
    }

    "relogio em minutos e segundos" {
        FurnaceClock.format(0) shouldBe "00:00"
        FurnaceClock.format(75) shouldBe "01:15"
    }
})
