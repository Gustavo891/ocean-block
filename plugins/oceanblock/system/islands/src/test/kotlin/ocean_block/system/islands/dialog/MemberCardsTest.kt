package ocean_block.system.islands.dialog

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.awt.image.BufferedImage

private const val SKIN_TONE = 0xFFC83232.toInt()

private fun portrait(): BufferedImage = BufferedImage(MemberCards.PORTRAIT_WIDTH, MemberCards.PORTRAIT_HEIGHT, BufferedImage.TYPE_INT_ARGB).apply {
    for (x in 0 until width) for (y in 0 until height) setRGB(x, y, SKIN_TONE)
}

class MemberCardsTest : StringSpec({

    "a fileira tem seis cartões lado a lado com espaço entre eles" {
        val image = MemberCards.render(emptyList())

        image.width shouldBe 6 * MemberCards.CARD_WIDTH + 5 * MemberCards.GAP
        image.height shouldBe MemberCards.CARD_HEIGHT
    }

    "cartão do dono pinta o fundo, a coroa, o ponto de online e o retrato" {
        val image = MemberCards.render(listOf(MemberCards.Card(portrait(), owner = true, online = true)))

        image.getRGB(MemberCards.CARD_WIDTH / 2, MemberCards.CARD_HEIGHT - 3) shouldBe MemberCards.FILL
        image.getRGB(0, 0) shouldBe MemberCards.BORDER
        image.getRGB(MemberCards.CROWN_X + 1, MemberCards.CROWN_Y + 1) shouldBe MemberCards.CROWN
        image.getRGB(MemberCards.CARD_WIDTH - 4, 3) shouldBe MemberCards.ONLINE
        image.getRGB(MemberCards.CARD_WIDTH / 2, MemberCards.PORTRAIT_Y + 5) shouldBe SKIN_TONE
    }

    "membro offline não tem coroa e o ponto fica cinza" {
        val image = MemberCards.render(listOf(null, MemberCards.Card(portrait(), owner = false, online = false)))
        val left = MemberCards.CARD_WIDTH + MemberCards.GAP

        image.getRGB(left + MemberCards.CROWN_X + 1, MemberCards.CROWN_Y + 1) shouldNotBe MemberCards.CROWN
        image.getRGB(left + MemberCards.CARD_WIDTH - 4, 3) shouldBe MemberCards.OFFLINE
    }

    "vaga livre deixa o fundo transparente para a borda tracejada e desenha o mais" {
        val image = MemberCards.render(listOf(null))
        val center = MemberCards.CARD_WIDTH / 2

        (image.getRGB(3, MemberCards.CARD_HEIGHT - 3) ushr 24) shouldBe 0
        image.getRGB(center, MemberCards.PLUS_Y) shouldBe MemberCards.PLUS
        image.getRGB(center - 4, MemberCards.PLUS_Y) shouldBe MemberCards.PLUS
    }
})
