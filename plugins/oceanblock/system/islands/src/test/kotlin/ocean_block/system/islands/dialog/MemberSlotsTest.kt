package ocean_block.system.islands.dialog

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.awt.image.BufferedImage

private const val FACE = 0xFFC83232.toInt()
private const val HAT = 0xFF3232C8.toInt()

private fun skin(hat: Boolean): BufferedImage = BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB).apply {
    for (x in 8 until 16) for (y in 8 until 16) setRGB(x, y, FACE)
    if (hat) for (x in 40 until 48) for (y in 8 until 12) setRGB(x, y, HAT)
}

class MemberSlotsTest : StringSpec({

    "o rosto da skin vira 8x8 com o chapéu por cima" {
        val face = MemberSlots.face(skin(hat = true))

        face.width shouldBe 8
        face.height shouldBe 8
        face.getRGB(3, 1) shouldBe HAT
        face.getRGB(3, 6) shouldBe FACE
    }

    "vaga com jogador mostra a cabeça em 2x e vaga livre mostra um mais" {
        val face = MemberSlots.face(skin(hat = false))

        val image = MemberSlots.row(listOf(face, null), total = 2)

        image.width shouldBe 2 * MemberSlots.SLOT
        image.height shouldBe MemberSlots.SLOT
        image.getRGB(1, 1) shouldBe FACE
        image.getRGB(16, 16) shouldBe FACE
        (image.getRGB(0, 0) ushr 24) shouldBe 0
        val center = MemberSlots.SLOT + MemberSlots.SLOT / 2
        image.getRGB(center, MemberSlots.SLOT / 2) shouldBe MemberSlots.PLUS
        image.getRGB(center - 3, MemberSlots.SLOT / 2) shouldBe MemberSlots.PLUS
        (image.getRGB(MemberSlots.SLOT + 3, 3) ushr 24) shouldBe 0
    }

    "faltando rostos na lista as vagas restantes também ganham mais" {
        val image = MemberSlots.row(emptyList(), total = 3)

        image.getRGB(2 * MemberSlots.SLOT + MemberSlots.SLOT / 2, MemberSlots.SLOT / 2) shouldBe MemberSlots.PLUS
    }
})
