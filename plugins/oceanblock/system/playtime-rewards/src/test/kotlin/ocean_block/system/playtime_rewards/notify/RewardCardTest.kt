package ocean_block.system.playtime_rewards.notify

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith

class RewardCardTest : StringSpec({

    "o cartão escolhe a menor largura que cabe o texto mais largo" {
        RewardCard.width(listOf("a")) shouldBe 110
        RewardCard.width(listOf("x".repeat(25))) shouldBe 200
        RewardCard.width(listOf("x".repeat(80))) shouldBe 240
    }

    "linha 1 desenha o cartão, volta e escreve o tempo em aqua" {
        val lines = RewardCard.lines(240)
        lines shouldHaveSize 2
        val title = "Recompensa de 4 h!"
        val width = RewardCard.width(listOf(title, RewardCard.HINT))
        lines[0] shouldBe "<shadow:#00000000><white><glyph:tempo_cartao_$width></white></shadow><shift:-${width - 22}>" +
            "<aqua>$title</aqua><shift:${width - 22 - LineText.width(title)}>"
    }

    "linha 2 começa depois do ícone e diz onde resgatar em verde" {
        val lines = RewardCard.lines(30)
        val width = RewardCard.width(listOf("Recompensa de 30 min!", RewardCard.HINT))
        lines[1] shouldStartWith "<shift:22><green>${RewardCard.HINT}</green>"
        lines[1] shouldBe "<shift:22><green>${RewardCard.HINT}</green><shift:${width - 22 - LineText.width(RewardCard.HINT)}>"
    }

    "largura segue a fonte vanilla" {
        LineText.width("i") shouldBe 2
        LineText.width("a") shouldBe 6
        LineText.width("a a") shouldBe 16
        LineText.width("Il") shouldBe 7
    }
})
