package ocean_block.system.machines.furnace

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class FurnaceBarTest : StringSpec({

    fun bar(progress: Int) = (0 until 5).map { FurnaceBar.segmentFill(progress, 50, it, 5) }

    "barra vazia no inicio e cheia no fim" {
        bar(0) shouldBe listOf(0, 0, 0, 0, 0)
        bar(50) shouldBe listOf(100, 100, 100, 100, 100)
    }

    "cada segmento enche em quartos" {
        bar(25) shouldBe listOf(100, 100, 50, 0, 0)
        bar(13) shouldBe listOf(100, 25, 0, 0, 0)
    }

    "progresso fora da faixa fica nos limites" {
        bar(-5) shouldBe listOf(0, 0, 0, 0, 0)
        bar(80) shouldBe listOf(100, 100, 100, 100, 100)
    }
})
