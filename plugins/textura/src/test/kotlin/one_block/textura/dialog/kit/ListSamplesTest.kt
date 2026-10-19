package one_block.textura.dialog.kit

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import one_block.textura.dialog.DialogFixtures

class ListSamplesTest : StringSpec({

    "cada lista com perPage gera as contagens de 0 a perPage" {
        val definition = DialogFixtures.parse(
            """
            {"theme": "stone", "column": [
              {"grid": [{"repeat": "receitas", "perPage": 6, "column": [{"text": "{nome}"}]}], "columns": 2},
              {"row": [{"repeat": "chips", "perPage": 4, "chip": "{qtd}"}]}
            ]}
            """.trimIndent(),
        ).first!!
        val warnings = mutableListOf<String>()

        val limits = ListSamples.limits(definition, warnings::add)

        limits shouldBe mapOf("receitas" to 6, "chips" to 4)
        warnings.shouldBeEmpty()
        val combinations = ListSamples.combinations(limits)
        combinations shouldHaveSize 7 * 5
        combinations.map { it.getValue("receitas") }.toSet() shouldBe (0..6).toSet()
        combinations.map { it.getValue("chips") }.toSet() shouldBe (0..4).toSet()
        combinations.toSet() shouldHaveSize 35
    }

    "lista sem perPage avisa e usa 1" {
        val definition = DialogFixtures.parse(
            """{"theme": "stone", "column": [{"repeat": "categorias", "nav": "{nome}"}]}""",
        ).first!!
        val warnings = mutableListOf<String>()

        val limits = ListSamples.limits(definition, warnings::add)

        limits shouldBe mapOf("categorias" to 1)
        warnings.single() shouldContain "categorias"
        ListSamples.combinations(limits) shouldContainExactlyInAnyOrder listOf(mapOf("categorias" to 0), mapOf("categorias" to 1))
    }

    "a mesma lista em dois nos usa o maior perPage" {
        val definition = DialogFixtures.parse(
            """{"theme": "stone", "column": [{"repeat": "l", "perPage": 2, "text": "a"}, {"repeat": "l", "perPage": 5, "text": "b"}]}""",
        ).first!!

        ListSamples.limits(definition) { } shouldBe mapOf("l" to 5)
    }

    "tela sem repeat compila uma vez so" {
        ListSamples.combinations(emptyMap()) shouldContainExactly listOf(emptyMap())
    }

    "produto grande demais varia uma lista por vez" {
        val limits = mapOf("a" to 20, "b" to 20, "c" to 20)
        val warnings = mutableListOf<String>()

        val combinations = ListSamples.combinations(limits, warnings::add)

        combinations.toSet() shouldHaveSize combinations.size
        listOf("a", "b", "c").forEach { key ->
            (0..20).forEach { count ->
                combinations.any { it.getValue(key) == count } shouldBe true
            }
        }
        (combinations.size < 21 * 21 * 21) shouldBe true
        warnings.single() shouldContain "combinações"
    }
})
