package ocean_block.system.islands.dialog

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.awt.image.BufferedImage

private const val FACE = 0xFFC83232.toInt()
private const val HAT = 0xFF3232C8.toInt()

private fun skin(hat: Boolean): BufferedImage = BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB).apply {
    for (x in 8 until 16) for (y in 8 until 16) setRGB(x, y, FACE)
    if (hat) for (x in 40 until 48) for (y in 8 until 12) setRGB(x, y, HAT)
}

class MemberCardsTest : StringSpec({

    "o rosto da skin vira 8x8 com o chapéu por cima" {
        val face = MemberCards.face(skin(hat = true))

        face.width shouldBe 8
        face.height shouldBe 8
        face.getRGB(3, 1) shouldBe HAT
        face.getRGB(3, 6) shouldBe FACE
    }

    "a fileira tem seis cartões lado a lado com espaço entre eles" {
        val image = MemberCards.render(emptyList())

        image.width shouldBe 6 * MemberCards.CARD_WIDTH + 5 * MemberCards.GAP
        image.height shouldBe MemberCards.CARD_HEIGHT
    }

    "cartão do dono pinta o fundo, a coroa, o ponto de online e o rosto em 2x" {
        val image = MemberCards.render(listOf(MemberCards.Card(MemberCards.face(skin(hat = false)), owner = true, online = true)))
        val faceLeft = (MemberCards.CARD_WIDTH - 16) / 2

        image.getRGB(MemberCards.CARD_WIDTH / 2, MemberCards.CARD_HEIGHT - 3) shouldBe MemberCards.FILL
        image.getRGB(0, 0) shouldBe MemberCards.BORDER
        image.getRGB(MemberCards.CROWN_X + 1, MemberCards.CROWN_Y + 1) shouldBe MemberCards.CROWN
        image.getRGB(MemberCards.CARD_WIDTH - 4, 3) shouldBe MemberCards.ONLINE
        image.getRGB(faceLeft, MemberCards.FACE_Y) shouldBe FACE
        image.getRGB(faceLeft + 15, MemberCards.FACE_Y + 15) shouldBe FACE
    }

    "membro offline não tem coroa e o ponto fica cinza" {
        val image = MemberCards.render(listOf(null, MemberCards.Card(MemberCards.face(skin(hat = false)), owner = false, online = false)))
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
