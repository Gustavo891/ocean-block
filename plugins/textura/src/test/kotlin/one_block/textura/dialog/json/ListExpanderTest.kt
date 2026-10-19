package one_block.textura.dialog.json

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import one_block.textura.dialog.DialogFixtures

class ListExpanderTest : StringSpec({

    val screen = """
        {
          "theme": "stone",
          "column": [
            {"text": "topo"},
            {"grid": [
              {"repeat": "receitas", "perPage": 3, "column": [{"text": "{nome}"}]}
            ], "columns": 2},
            {"text": "rodape"}
          ]
        }
    """.trimIndent()

    val entries = (0 until 8).map { mapOf("nome" to "R$it") }

    fun grid(root: DefContainer): DefContainer = root.children[1] as DefContainer

    fun names(container: DefContainer): List<String> = container.children.map {
        val scoped = it as DefScoped
        scoped.values.getValue("nome")
    }

    "repeat vira um no por entrada da pagina, limitado por perPage" {
        val definition = DialogFixtures.parse(screen).first!!
        val expanded = ListExpander({ entries }, { 1 }).expand(definition.root)

        names(grid(expanded)) shouldContainExactly listOf("R3", "R4", "R5")
        grid(expanded).children.map { (it as DefScoped).index } shouldContainExactly listOf(3, 4, 5)
        expanded.children.size shouldBe 3
    }

    "ultima pagina parcial mostra so o que sobra" {
        val definition = DialogFixtures.parse(screen).first!!
        val expanded = ListExpander({ entries }, { 2 }).expand(definition.root)

        names(grid(expanded)) shouldContainExactly listOf("R6", "R7")
    }

    "pagina fora do intervalo e limitada a primeira ou a ultima" {
        val definition = DialogFixtures.parse(screen).first!!

        names(grid(ListExpander({ entries }, { 9 }).expand(definition.root))) shouldContainExactly listOf("R6", "R7")
        names(grid(ListExpander({ entries }, { -4 }).expand(definition.root))) shouldContainExactly listOf("R0", "R1", "R2")
    }

    "lista ausente ou vazia nao gera nos" {
        val definition = DialogFixtures.parse(screen).first!!

        grid(ListExpander({ emptyList() }, { 0 }).expand(definition.root)).children.shouldBeEmpty()
    }

    "sem perPage mostra todas as entradas e ignora a pagina" {
        val (definition, _) = DialogFixtures.parse(
            """{"theme": "stone", "row": [{"repeat": "chips", "chip": "{qtd}"}]}""",
        )
        val expanded = ListExpander({ (1..5).map { n -> mapOf("qtd" to "$n") } }, { 3 }).expand(definition!!.root)

        expanded.children.map { (it as DefScoped).values.getValue("qtd") } shouldContainExactly listOf("1", "2", "3", "4", "5")
    }

    "repeticao aninhada herda os campos de fora e a entrada de dentro vence" {
        val (definition, _) = DialogFixtures.parse(
            """
            {"theme": "stone", "column": [
              {"repeat": "fora", "column": [
                {"repeat": "dentro", "text": "{a} {b}"}
              ]}
            ]}
            """.trimIndent(),
        )
        val lists = mapOf(
            "fora" to listOf(mapOf("a" to "A1", "b" to "fora")),
            "dentro" to listOf(mapOf("b" to "B1"), mapOf("b" to "B2")),
        )
        val expanded = ListExpander({ lists[it].orEmpty() }, { 0 }).expand(definition!!.root)

        val outer = expanded.children.single().shouldBeInstanceOf<DefScoped>()
        val inner = (outer.node as DefContainer).children.map { it.shouldBeInstanceOf<DefScoped>() }
        inner.map { it.values } shouldContainExactly listOf(mapOf("a" to "A1", "b" to "B1"), mapOf("a" to "A1", "b" to "B2"))
        inner.map { it.index } shouldContainExactly listOf(0, 1)
    }

    "fornecedor da lista e chamado uma vez por expansao" {
        val definition = DialogFixtures.parse(
            """{"theme": "stone", "column": [{"repeat": "l", "text": "a"}, {"repeat": "l", "text": "b"}]}""",
        ).first!!
        var calls = 0
        ListExpander({ calls++; listOf(emptyMap()) }, { 0 }).expand(definition.root)

        calls shouldBe 1
    }
})
