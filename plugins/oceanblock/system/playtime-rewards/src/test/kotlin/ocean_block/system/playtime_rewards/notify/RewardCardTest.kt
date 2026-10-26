package ocean_block.system.playtime_rewards.notify

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class RewardCardTest : StringSpec({

    "o toast de tempo tem o fundo na primeira linha, titulo amarelo e a dica em branco" {
        val title = "4 h online!"
        val hint = RewardCard.HINT

        RewardCard.lines(240) shouldContainExactly listOf(
            "<shadow:#00000000><white><glyph:toast_tempo></white><shift:-130><yellow>$title</yellow>" +
                "<shift:${130 - LineText.width(title)}></shadow>",
            "<shadow:#00000000><shift:30><white>$hint</white><shift:${130 - LineText.width(hint)}></shadow>",
        )
    }

    "a dica cabe no toast" {
        (LineText.width(RewardCard.HINT) <= 130) shouldBe true
        RewardCard.HINT shouldBe "Resgate: /recompensas"
    }

    "largura segue a fonte vanilla" {
        LineText.width("i") shouldBe 2
        LineText.width("a") shouldBe 6
        LineText.width("a a") shouldBe 16
        LineText.width("Il") shouldBe 7
    }
})
