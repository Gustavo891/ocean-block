package ocean_block.system.playtime_rewards.notify

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private val bundled = setOf(30, 60, 120, 240, 480, 720, 1440, 2880, 4320, 6000).map { "tempo_faixa_$it" }.toSet()

class RewardBannerTest : StringSpec({

    "marco com peça própria usa a faixa com o tempo" {
        RewardBanner.textPiece(240) { it in bundled } shouldBe "tempo_faixa_240"
    }

    "marco sem peça usa a faixa genérica" {
        RewardBanner.textPiece(90) { it in bundled } shouldBe "tempo_faixa_generico"
    }

    "title junta pontas e texto encostando cada peça na anterior" {
        RewardBanner.title(240, 1) { it in bundled } shouldBe
            "<white><glyph:tempo_faixa_esq_1><shift:-1><glyph:tempo_faixa_240><shift:-1><glyph:tempo_faixa_dir_1>"
    }

    "linhas saem centralizadas pela metade da largura" {
        RewardBanner.lines() shouldContainExactly listOf(
            "<shift:-${LineText.width(RewardBanner.LINE_1) / 2}><gold>${RewardBanner.LINE_1}",
            "<shift:-${LineText.width(RewardBanner.LINE_2) / 2}><green>${RewardBanner.LINE_2}",
        )
    }

    "largura segue a fonte vanilla" {
        LineText.width("i") shouldBe 2
        LineText.width("a") shouldBe 6
        LineText.width("a a") shouldBe 16
        LineText.width("Il") shouldBe 7
    }
})
