package textura_studio

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import textura_studio.render.GlyphRequest
import textura_studio.render.HudRequest
import textura_studio.render.HudSample
import textura_studio.render.SampleLine
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

class SectionsTest : FunSpec({

    val data = File(checkNotNull(System.getProperty("studio.data")))
    val studio = Studio(data)

    test("notificações: região à direita termina na margem e as linhas descem 11 + espaçamento") {
        val yaml = """
            coleta:
              ancora: MEIO_DIREITA
              x: 0
              y: 70
              linhas: 6
              espacamento: 1
        """.trimIndent()
        val result = studio.hudPreview(
            HudRequest(yaml, mapOf("coleta" to listOf(SampleLine("<green>+3</green> Tábuas", "item:oak_planks"))), null, 1920, 1080, 0),
        )
        val box = result.regions.single()

        result.scale shouldBe 4
        (box.x + box.w) shouldBe result.guiWidth - 2
        box.y shouldBe result.guiHeight / 2 + 70 - 2
        box.h shouldBe 5 * 12 + 11
        ImageIO.write(result.image, "png", File(System.getProperty("user.dir"), "build/previews/hud.png").also { it.parentFile.mkdirs() })
    }

    test("notificações: âncora inválida vira aviso e a região some") {
        val result = studio.hudPreview(HudRequest("x:\n  ancora: CENTRO\n", emptyMap(), null, 1280, 720, 2))
        result.regions shouldBe emptyList()
        result.warnings.joinToString() shouldContain "inválida"
    }

    test("HUD da bossbar desenha na segunda barra, centralizada") {
        val result = studio.hudPreview(HudRequest("", emptyMap(), HudSample("<yellow>Ilha</yellow> nível 3", null, false, 6), 1920, 1080, 0))
        val hud = checkNotNull(result.hud)
        (hud.x + hud.w / 2 - result.guiWidth / 2) shouldBe 0
        hud.w shouldBeGreaterThan 20
    }

    test("glyph no chat sai com a altura pedida") {
        val image = studio.glyphPreview(
            GlyphRequest(File(data, "resource/assets/oneblock/textures/hud/left.png"), ascent = 6, height = 14, size = 6, text = "A {glyph} B"),
        )
        image.height shouldBeGreaterThan 14 * 4
    }

    test("arquivos: só caminhos liberados") {
        studio.files.list("configs/hud", "yml") shouldContain "configs/hud/notificacoes.yml"
        shouldThrow<StudioFileException> { studio.files.read("config.yml") }
        shouldThrow<StudioFileException> { studio.files.read("configs/../config.yml") }
        shouldThrow<StudioFileException> { studio.files.writeText("configs/fonts/custom.json", "{}") }
        shouldThrow<StudioFileException> { studio.files.writeText("configs/hud/x.yml", "a: [") }
    }

    test("arquivos: grava yml e PNG numa pasta temporária") {
        val folder = Files.createTempDirectory("studio-files").toFile()
        val files = StudioFiles(folder)
        files.writeText("configs/hud/notificacoes.yml", "a:\n  ancora: TOPO_ESQUERDA\n")
        files.read("configs/hud/notificacoes.yml") shouldContain "TOPO_ESQUERDA"
        val png = File(data, "resource/assets/oneblock/textures/hud/left.png").readBytes()
        files.writeImage("resource/assets/oneblock/textures/icons/novo.png", png)
        File(folder, "resource/assets/oneblock/textures/icons/novo.png").isFile shouldBe true
        shouldThrow<StudioFileException> { files.writeImage("resource/assets/oneblock/textures/icons/x.png", "nada".toByteArray()) }
        folder.deleteRecursively()
    }
})
