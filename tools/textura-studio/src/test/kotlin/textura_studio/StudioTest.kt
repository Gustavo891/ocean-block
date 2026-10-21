package textura_studio

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import textura_studio.render.PreviewRequest
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

class StudioTest : FunSpec({

    val data = File(checkNotNull(System.getProperty("studio.data")))
    val studio = Studio(data)

    test("lista as telas do servidor") {
        studio.store.list().map { it.id } shouldContain "shop"
    }

    test("preview da shop sai no tamanho do monitor com corpo e áreas clicáveis") {
        val json = checkNotNull(studio.store.read("shop"))
        val result = studio.preview(PreviewRequest("shop", json, monitorWidth = 1920, monitorHeight = 1080, scale = 0))

        result.error.shouldBeNull()
        val image = result.image.shouldNotBeNull()
        image.width shouldBe 1920
        image.height shouldBe 1080
        result.scale shouldBe 4
        result.guiWidth shouldBe 480
        result.body.shouldNotBeNull()
        result.theme shouldBe "stone"
    }

    test("caixas do preview levam o caminho do parser, com instâncias de lista indexadas") {
        val json = checkNotNull(studio.store.read("receitas"))
        val result = studio.preview(PreviewRequest("receitas", json, lists = mapOf("categorias" to List(2) { mapOf("nome" to "Cat $it") })))
        val tags = result.boxes.map { it.tag }

        tags shouldContain "row#0"
        tags.filter { it.startsWith("row#0 > Categorias > nav#0[") } shouldBe listOf("row#0 > Categorias > nav#0[0]", "row#0 > Categorias > nav#0[1]")
    }

    test("renderiza todas as telas sem erro") {
        val out = File(System.getProperty("user.dir"), "build/previews").also { it.mkdirs() }
        studio.store.list().forEach { summary ->
            val json = checkNotNull(studio.store.read(summary.id))
            val result = studio.preview(PreviewRequest(summary.id, json, monitorWidth = 1920, monitorHeight = 1080, scale = 3))
            result.error.shouldBeNull()
            ImageIO.write(result.image, "png", File(out, "${summary.id}.png"))
        }
    }

    test("JSON quebrado devolve erro no preview e não grava") {
        val folder = Files.createTempDirectory("studio-store").toFile()
        val store = DialogStore(folder)

        val result = studio.preview(PreviewRequest("quebrada", "{ \"row\": [", monitorWidth = 1280, monitorHeight = 720))
        result.error.shouldNotBeNull() shouldContain "JSON"

        shouldThrow<InvalidDialogException> { store.write("quebrada", "{ \"row\": [") }
        File(folder, "quebrada.json").exists() shouldBe false
        folder.deleteRecursively()
    }

    test("grava e lê de volta; recusa id com caminho") {
        val folder = Files.createTempDirectory("studio-store").toFile()
        val store = DialogStore(folder)

        store.write("teste", "{\"row\": [], \"theme\": \"stone\"}")
        store.read("teste") shouldBe "{\"row\": [], \"theme\": \"stone\"}"
        shouldThrow<InvalidDialogException> { store.write("../fora", "{}") }
        folder.deleteRecursively()
    }
})
