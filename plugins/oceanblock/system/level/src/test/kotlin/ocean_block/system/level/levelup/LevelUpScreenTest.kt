package ocean_block.system.level.levelup

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class LevelUpScreenTest : StringSpec({

    "cada nivel subido rende 100 vezes o nivel em coins" {
        LevelUpRewards.coins(1, 2) shouldBe 200.0
        LevelUpRewards.coins(1, 4) shouldBe 900.0
        LevelUpRewards.coins(5, 5) shouldBe 0.0
    }

    "texto mede pela fonte vanilla" {
        RewardText.width("i") shouldBe 2
        RewardText.width("Coins") shouldBe 26
        RewardText.width("a b") shouldBe 16
    }

    "o cartao usa a menor largura em que o texto cabe" {
        LevelUpCard.width(listOf("Nível 7!", "+700 Coins")) shouldBe 110
        LevelUpCard.width(listOf("Nível 12!", "+1.200 Coins", "Liberado: Rede de Detritos")) shouldBe 170
        LevelUpCard.width(listOf("x".repeat(80))) shouldBe 240
    }

    "sem receita o cartao tem duas linhas e o fundo vai na primeira" {
        val texts = listOf("Nível 7!", "+700 Coins")
        val w = 110
        val pad = LevelUpCard.PADDING

        LevelUpCard.lines(7, 700.0, emptyList()) shouldContainExactly listOf(
            "<shadow:#00000000><white><glyph:nivel_cartao_2_$w></white></shadow><shift:-${w - pad}>" +
                "<aqua>${texts[0]}</aqua><shift:${w - pad - RewardText.width(texts[0])}>",
            "<shift:$pad><yellow>${texts[1]}</yellow><shift:${w - pad - RewardText.width(texts[1])}>",
        )
    }

    "receita liberada vira a terceira linha e o cartao de tres linhas" {
        val lines = LevelUpCard.lines(12, 1200.0, listOf("Rede de Detritos"))

        lines.size shouldBe 3
        lines[0].startsWith("<shadow:#00000000><white><glyph:nivel_cartao_3_170>") shouldBe true
        lines[2].contains("<green>Liberado: Rede de Detritos</green>") shouldBe true
    }

    "todas as larguras e alturas tem glyph" {
        LevelUpCard.glyphIds() shouldBe LevelUpCard.WIDTHS.flatMap { w -> listOf("nivel_cartao_2_$w", "nivel_cartao_3_$w") }
    }
})
