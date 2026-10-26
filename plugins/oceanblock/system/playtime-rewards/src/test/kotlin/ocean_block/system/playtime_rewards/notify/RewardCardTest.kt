package ocean_block.system.playtime_rewards.notify

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class RewardCardTest : StringSpec({

    "o toast de tempo tem titulo amarelo sobre o fundo e a dica em branco na linha de baixo" {
        RewardCard.lines(240) shouldContainExactly listOf(
            ToastLine("<shadow:#00000000><yellow>4 h online!</yellow></shadow>", "glyph:toast_tempo"),
            ToastLine("<shadow:#00000000><white>Use /recompensas</white></shadow>", "glyph:toast_linha"),
        )
    }

    "a dica cabe no toast menor" {
        (LineText.width(RewardCard.HINT) <= 106) shouldBe true
    }

    "largura segue a fonte vanilla" {
        LineText.width("i") shouldBe 2
        LineText.width("a") shouldBe 6
        LineText.width("a a") shouldBe 16
        LineText.width("Il") shouldBe 7
    }
})
