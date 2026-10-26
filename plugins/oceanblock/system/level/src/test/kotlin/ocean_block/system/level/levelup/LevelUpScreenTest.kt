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

    "o toast de nivel tem titulo amarelo sobre o fundo e as coins em branco na linha de baixo" {
        LevelUpCard.lines(7, 700.0) shouldContainExactly listOf(
            ToastLine("<shadow:#00000000><yellow>Nível 7!</yellow></shadow>", "glyph:toast_nivel"),
            ToastLine("<shadow:#00000000><white>+700 Coins</white></shadow>", "glyph:toast_linha"),
        )
    }

    "sem coins o toast fica so com o titulo" {
        LevelUpCard.lines(50, 0.0).size shouldBe 1
    }
})
