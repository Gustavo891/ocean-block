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

    test("notificações: região à direita termina na margem e as linhas descem 13 + espaçamento") {
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
        box.y shouldBe result.guiHeight / 2 + 70 - 3
        box.h shouldBe 5 * 14 + 13
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

    test("blocos: catálogo lista as máquinas com as faces e a picareta") {
        val entries = BlockCatalog(data).entries()
        val gerador = entries.single { it.id == "gerador_energia" }
        gerador.kind shouldBe "bloco"
        gerador.faces.map { it.face } shouldBe listOf("top", "bottom", "side")
        gerador.faces.first().path shouldBe "resource/assets/oneblock/textures/block/maquinas/gerador_energia_top.png"
        entries.single { it.id == "minerador" }.faces.map { it.face } shouldContain "broca"
        entries.single { it.id == "picareta_fundidora" }.kind shouldBe "item"
        entries.flatMap { it.faces }.forEach { File(data, it.path).isFile shouldBe true }
    }

    test("blocos: tamanho aceito é 16x16 ou o tamanho atual, até 256") {
        BlockCatalog.sizeProblem(16, 16, 16, 16) shouldBe null
        BlockCatalog.sizeProblem(32, 32, 32, 32) shouldBe null
        BlockCatalog.sizeProblem(32, 32, 16, 16) shouldBe null
        (BlockCatalog.sizeProblem(16, 16, 32, 32) ?: "") shouldContain "16x16"
        (BlockCatalog.sizeProblem(null, null, 512, 512) ?: "") shouldContain "256"
        BlockCatalog.sizeProblem(null, null, 64, 64) shouldBe null
    }

    test("blocos: grava só texturas do catálogo e no tamanho certo") {
        val folder = Files.createTempDirectory("studio-blocos").toFile()
        File(folder, "configs/blocks").mkdirs()
        File(folder, "configs/blocks/maquinas.yml").writeText(
            "caixa:\n  type: CUSTOM\n  textures:\n    side: assets/oneblock/textures/block/caixa_side.png\n",
        )
        val png = File(data, "resource/assets/oneblock/textures/block/maquinas/gerador_energia_side.png").readBytes()
        File(folder, "resource/assets/oneblock/textures/block").mkdirs()
        File(folder, "resource/assets/oneblock/textures/block/caixa_side.png").writeBytes(png)
        val catalog = BlockCatalog(folder)
        val files = StudioFiles(folder)

        catalog.write(files, "resource/assets/oneblock/textures/block/caixa_side.png", png)
        shouldThrow<StudioFileException> { catalog.write(files, "resource/assets/oneblock/textures/block/outra.png", png) }
        val big = java.io.ByteArrayOutputStream().also { ImageIO.write(java.awt.image.BufferedImage(32, 32, 2), "png", it) }.toByteArray()
        shouldThrow<StudioFileException> { catalog.write(files, "resource/assets/oneblock/textures/block/caixa_side.png", big) }
        folder.deleteRecursively()
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
