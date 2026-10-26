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

    "o toast de nivel tem o fundo na primeira linha, titulo roxo e as coins em preto" {
        val title = "Nível 7!"
        val coins = "+700 Coins"

        LevelUpCard.lines(7, 700.0) shouldContainExactly listOf(
            "<shadow:#00000000><white><glyph:toast_nivel></white><shift:-130><dark_purple>$title</dark_purple>" +
                "<shift:${130 - RewardText.width(title)}></shadow>",
            "<shadow:#00000000><shift:30><black>$coins</black><shift:${130 - RewardText.width(coins)}></shadow>",
        )
    }

    "sem coins o toast fica so com o titulo" {
        LevelUpCard.lines(50, 0.0).size shouldBe 1
    }
})
