package ocean_block.system.drift.collect

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CollectTallyTest : StringSpec({

    "primeira coleta comeca na quantidade recebida" {
        val tally = CollectTally(windowTicks = 60)

        tally.add("tabua", 2, nowTick = 0) shouldBe 2
    }

    "coleta dentro da janela soma na mesma chave" {
        val tally = CollectTally(windowTicks = 60)

        tally.add("tabua", 2, nowTick = 0)
        tally.add("tabua", 3, nowTick = 59) shouldBe 5
    }

    "cada coleta reinicia a janela" {
        val tally = CollectTally(windowTicks = 60)

        tally.add("tabua", 1, nowTick = 0)
        tally.add("tabua", 1, nowTick = 50)
        tally.add("tabua", 1, nowTick = 100) shouldBe 3
    }

    "coleta depois da janela recomeca do zero" {
        val tally = CollectTally(windowTicks = 60)

        tally.add("tabua", 4, nowTick = 0)
        tally.add("tabua", 1, nowTick = 60) shouldBe 1
    }

    "chaves diferentes nao se misturam" {
        val tally = CollectTally(windowTicks = 60)

        tally.add("tabua", 4, nowTick = 0)
        tally.add("linha", 1, nowTick = 10) shouldBe 1
        tally.add("tabua", 1, nowTick = 20) shouldBe 5
    }

    "entradas vencidas sao descartadas" {
        val tally = CollectTally(windowTicks = 60)

        tally.add("tabua", 4, nowTick = 0)
        tally.add("linha", 1, nowTick = 100)

        tally.size shouldBe 1
    }
})
