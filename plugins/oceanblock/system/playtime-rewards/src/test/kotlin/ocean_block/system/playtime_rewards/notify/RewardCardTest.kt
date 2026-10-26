package ocean_block.system.playtime_rewards.notify

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class RewardCardTest : StringSpec({

    "o toast de tempo tem titulo laranja ao lado do selo e a dica em branco na linha de baixo" {
        RewardCard.lines(240) shouldContainExactly listOf(
            ToastLine("<gold>4 h online!</gold>", "glyph:toast_tempo"),
            ToastLine("<white>Use /recompensas</white>", "glyph:toast_linha"),
        )
    }

    "a dica cabe no corpo do toast" {
        (LineText.width(RewardCard.HINT) <= 96) shouldBe true
    }

    "largura segue a fonte vanilla" {
        LineText.width("i") shouldBe 2
        LineText.width("a") shouldBe 6
        LineText.width("a a") shouldBe 16
        LineText.width("Il") shouldBe 7
    }
})
