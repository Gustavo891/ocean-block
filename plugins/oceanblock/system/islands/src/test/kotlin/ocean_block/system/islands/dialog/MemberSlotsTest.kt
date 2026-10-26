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

    "as vagas ficam centralizadas na largura total e só as ocupadas ganham cabeça" {
        val face = MemberSlots.face(skin(hat = false))

        val image = MemberSlots.row(listOf(face, null), capacity = 3, total = 6)
        val start = (6 - 3) * MemberSlots.SLOT / 2

        image.width shouldBe 6 * MemberSlots.SLOT
        image.height shouldBe MemberSlots.SLOT
        image.getRGB(start + 1, 1) shouldBe FACE
        image.getRGB(start + 16, 16) shouldBe FACE
        (image.getRGB(start, 0) ushr 24) shouldBe 0
        (image.getRGB(1, 1) ushr 24) shouldBe 0
        (image.getRGB(start + MemberSlots.SLOT + 5, 5) ushr 24) shouldBe 0
    }
})
