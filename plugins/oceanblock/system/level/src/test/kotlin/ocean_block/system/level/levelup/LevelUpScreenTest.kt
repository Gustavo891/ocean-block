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

    "texto normal mede pela fonte vanilla" {
        RewardText.width("i") shouldBe 2
        RewardText.width("Coins") shouldBe 26
        RewardText.width("a b") shouldBe 16
    }

    "o titulo e a faixa dourada" {
        LevelUpScreen.title() shouldBe "<white><glyph:nivel_faixa>"
    }

    "o subtitulo e o numero entre os brilhos do quadro" {
        LevelUpScreen.subtitle(12, 1) shouldBe
            "<white><glyph:nivel_brilho_e_1><glyph:nivel_num_1><glyph:nivel_num_2><glyph:nivel_brilho_d_1>"
    }

    "as recompensas viram linhas centradas pela metade da largura" {
        val coins = "+1.200 Coins"
        val unlocked = "Liberado: Rede de Detritos"

        LevelUpScreen.rewardLines(1200.0, listOf("Rede de Detritos")) shouldContainExactly listOf(
            "<shift:-${RewardText.width(coins) / 2}><gold>$coins",
            "<shift:-${RewardText.width(unlocked) / 2}><green>$unlocked",
        )
    }

    "sem coins e sem receita nao ha linhas" {
        LevelUpScreen.rewardLines(0.0, emptyList()) shouldBe emptyList()
    }
})
