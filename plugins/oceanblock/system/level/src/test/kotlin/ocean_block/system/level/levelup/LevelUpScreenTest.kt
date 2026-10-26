package ocean_block.system.level.levelup

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class LevelUpScreenTest : StringSpec({

    "cada nivel subido rende 100 vezes o nivel em coins" {
        LevelUpRewards.coins(1, 2) shouldBe 200.0
        LevelUpRewards.coins(1, 4) shouldBe 900.0
        LevelUpRewards.coins(5, 5) shouldBe 0.0
    }

    "texto pequeno mede pela metade da fonte vanilla" {
        SmallText.width("i") shouldBe 2
        SmallText.width("Coins") shouldBe 18
        SmallText.width("a b") shouldBe 10
    }

    "o titulo e a faixa dourada" {
        LevelUpScreen.title() shouldBe "<white><glyph:nivel_faixa>"
    }

    "o numero fica entre os brilhos do quadro" {
        LevelUpScreen.number(12, 1) shouldBe
            "<glyph:nivel_brilho_e_1><glyph:nivel_num_1><glyph:nivel_num_2><glyph:nivel_brilho_d_1>"
    }

    "as linhas de recompensa ficam centradas embaixo do numero e devolvem o cursor" {
        val coins = "+200 Coins"
        val r1 = SmallText.width(coins)
        val n = LevelUpScreen.numberWidth(2)
        val s1 = (n + r1) / 2

        LevelUpScreen.subtitle(2, 0, 200.0, emptyList()) shouldBe
            "<white>" + LevelUpScreen.number(2, 0) +
            "<shift:-$s1><font:oneblock:nivel_linha_1><gold>$coins</gold></font><shift:${s1 - r1}>"
    }

    "receitas liberadas entram na segunda linha" {
        val subtitle = LevelUpScreen.subtitle(3, 2, 300.0, listOf("Rede de Detritos"))
        val line = "Liberado: Rede de Detritos"
        val r2 = SmallText.width(line)
        val s2 = (LevelUpScreen.numberWidth(3) + r2) / 2

        subtitle.endsWith("<shift:-$s2><font:oneblock:nivel_linha_2><green>$line</green></font><shift:${s2 - r2}>") shouldBe true
    }

    "a largura do numero conta digitos e os dois brilhos" {
        LevelUpScreen.numberWidth(7) shouldBe 18
        LevelUpScreen.numberWidth(42) shouldBe 24
    }
})
