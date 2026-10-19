package one_block.textura.dialog

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import one_block.textura.api.DialogBinds
import one_block.textura.dialog.json.ScreenCompiler
import one_block.textura.dialog.kit.SurfaceHeight
import one_block.textura.dialog.kit.IconSize
import one_block.textura.dialog.kit.UiUsage
import one_block.textura.dialog.layout.LaidOut
import one_block.textura.dialog.layout.UiLayout

class ListCompileTest : StringSpec({

    val json = """
        {
          "theme": "stone",
          "column": [
            {"text": "{nome}"},
            {"grid": [
              {"repeat": "receitas", "perPage": 2, "column": [
                {"text": "{nome}"},
                {"button": "Craftar", "bind": "craftar", "disabled": "{falta}", "selected": "{marcada}"}
              ]}
            ], "columns": 2},
            {"row": [
              {"repeat": "categorias", "nav": "{nome}", "bind": "categoria", "selected": "{ativa}",
               "onClick": {"action": "run_command", "value": "[set] escolhida={id}"}}
            ]}
          ]
        }
    """.trimIndent()

    val receitas = listOf(
        mapOf("nome" to "Gancho", "falta" to "false", "marcada" to "true"),
        mapOf("nome" to "Rede", "falta" to "true"),
        mapOf("nome" to "Remo", "falta" to "nao"),
    )
    val categorias = listOf(
        mapOf("nome" to "Ferramentas", "id" to "ferramentas", "ativa" to "true"),
        mapOf("nome" to "Coleta", "id" to "coleta", "ativa" to "false"),
    )

    class Run(val laid: LaidOut, val session: DialogSession, val crafted: MutableList<Int>, val categories: MutableList<Int>)

    fun render(page: Int): Run {
        val definition = DialogFixtures.parse(json).first!!
        val crafted = mutableListOf<Int>()
        val categories = mutableListOf<Int>()
        val binds = DialogBinds().apply {
            text("nome") { "Global" }
            list("receitas") { receitas }
            listPage("receitas") { page }
            list("categorias") { categorias }
            listButton("craftar") { _, index -> crafted += index }
            listButton("categoria") { _, index -> categories += index }
        }
        val session = DialogSession(definition.id, binds, player = DialogFixtures.player())
        val screen = ScreenCompiler(DialogFixtures.kit) { }.compile(definition, DialogFixtures.theme, session)
        return Run(UiLayout(DialogFixtures.kit).layout(screen), session, crafted, categories)
    }

    "campos da entrada vencem os valores globais e fora da lista vale o global" {
        val texts = render(0).laid.texts.map { it.text }

        texts.first() shouldBe "Global"
        texts shouldContainAll listOf("Gancho", "Rede", "Ferramentas", "Coleta")
        texts shouldNotContain "Remo"
    }

    "pagina seguinte mostra o resto" {
        val texts = render(1).laid.texts.map { it.text }

        texts shouldContainAll listOf("Remo")
        texts shouldNotContain "Gancho"
    }

    "disabled e selected por placeholder escolhem a arte do botao" {
        val images = render(0).laid.images.map { it.id }

        images shouldContainAll listOf("ui:button_selected", "ui:button_disabled", "ui:nav_active", "ui:nav")
        render(1).laid.images.map { it.id } shouldContainAll listOf("ui:button")
    }

    "botao da lista chama listButton com o indice absoluto da entrada" {
        val first = render(0)
        val clickable = first.laid.hotspots.mapNotNull { it.action }
        clickable.forEach { it.run(DialogFixtures.player()) }

        first.crafted shouldContainExactly listOf(0)
        first.categories shouldContainExactly listOf(0, 1)

        val second = render(1)
        second.laid.hotspots.mapNotNull { it.action }.forEach { it.run(DialogFixtures.player()) }
        second.crafted shouldContainExactly listOf(2)
    }

    "acoes do onClick enxergam os campos da entrada" {
        val run = render(0)
        val navs = run.laid.hotspots.mapNotNull { it.action }.takeLast(2)

        navs[1].run(DialogFixtures.player())

        run.session.value("escolhida") shouldBe "coleta"
    }

    "gerar compila toda contagem de 0 a perPage e assa cada altura" {
        val definition = DialogFixtures.parse(
            """
            {"theme": "stone", "column": [
              {"grid": [{"repeat": "receitas", "perPage": 6, "text": "{nome}"}], "columns": 2, "surface": "sheet"},
              {"slots": [1, 1], "items": [{"at": 0, "item": "{item}"}]}
            ]}
            """.trimIndent(),
        ).first!!

        val usage = UiUsage.collect(listOf(definition), mapOf("stone" to DialogFixtures.theme), DialogFixtures.kit, setOf("fishing_rod")) { }

        val heights = usage.surfaces.filter { it.id == "ui:stone/sheet" }.map(SurfaceHeight::height).toSet()
        heights shouldBe setOf(0, 9, 18, 27)
        usage.icons shouldContainAll listOf(IconSize("item:fishing_rod", 8))
    }
})
