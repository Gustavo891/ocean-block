package one_block.textura.pack

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import one_block.textura.dialog.kit.IconScale
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.util.logging.Logger
import javax.imageio.ImageIO

class IconLibraryTest : StringSpec({

    "reduz renders grandes até o máximo da biblioteca" {
        val big = BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB)
        val small = IconResample.toMax(big, IconLibrary.MAX_SIZE)
        small.width shouldBe 128
        small.height shouldBe 128
        IconResample.toMax(BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), 128).width shouldBe 16
    }

    "lê itens e entidades pelo nome em maiúsculas" {
        val root = Files.createTempDirectory("biblioteca").toFile()
        File(root, "items").mkdirs()
        File(root, "entities/isometric").mkdirs()
        ImageIO.write(BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB), "png", File(root, "items/DIAMOND.png"))
        ImageIO.write(BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB), "png", File(root, "entities/isometric/PILLAGER.png"))
        val library = IconLibrary(root, Logger.getAnonymousLogger())

        library.item("diamond").shouldNotBeNull().width shouldBe 128
        library.entity("pillager").shouldNotBeNull().width shouldBe 64
        library.item("stone").shouldBeNull()
        library.item("../x").shouldBeNull()
    }

    "ícone grande encolhe suave para o tamanho pedido" {
        val resized = IconScale.resize(BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB), 32).shouldNotBeNull()
        resized.width shouldBe 32
        resized.height shouldBe 32
    }
})
