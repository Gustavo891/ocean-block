package one_block.textura.dialog.json

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import one_block.textura.dialog.DialogFixtures

class ScreenParserListTest : StringSpec({

    "repeat envolve o no com a lista e o perPage" {
        val (definition, warnings) = DialogFixtures.parse(
            """{"theme": "stone", "grid": [{"repeat": "receitas", "perPage": 6, "column": [{"text": "{nome}"}]}], "columns": 2}""",
        )

        val repeat = definition!!.root.children.single().shouldBeInstanceOf<DefRepeat>()
        repeat.list shouldBe "receitas"
        repeat.perPage shouldBe 6
        repeat.node.shouldBeInstanceOf<DefContainer>()
        warnings.shouldBeEmpty()
        definition.keys shouldContain "receitas"
    }

    "perPage sem repeat continua so ignorado" {
        val (definition, warnings) = DialogFixtures.parse(
            """{"theme": "stone", "column": [{"text": "a", "perPage": true}]}""",
        )

        definition!!.root.children.single().shouldBeInstanceOf<DefText>()
        warnings.shouldBeEmpty()
    }

    "perPage invalido com repeat avisa e mostra tudo" {
        val (definition, warnings) = DialogFixtures.parse(
            """{"theme": "stone", "column": [{"repeat": "l", "text": "a", "perPage": 0}]}""",
        )

        definition!!.root.children.single().shouldBeInstanceOf<DefRepeat>().perPage shouldBe null
        warnings.single() shouldContain "perPage"
    }

    "perPage acima de 64 e limitado com aviso" {
        val (definition, warnings) = DialogFixtures.parse(
            """{"theme": "stone", "column": [{"repeat": "l", "text": "a", "perPage": 500}]}""",
        )

        definition!!.root.children.single().shouldBeInstanceOf<DefRepeat>().perPage shouldBe 64
        warnings.single() shouldContain "64"
    }

    "disabled e selected aceitam placeholder e booleano" {
        val (definition, warnings) = DialogFixtures.parse(
            """
            {"theme": "stone", "column": [
              {"button": "Craftar", "bind": "craftar", "disabled": "{falta}", "selected": true},
              {"nav": "Ferramentas", "bind": "categoria", "selected": "{ativa}"}
            ]}
            """.trimIndent(),
        )

        val button = definition!!.root.children[0].shouldBeInstanceOf<DefButton>()
        button.disabled shouldBe DefFlag.Bound("{falta}")
        button.selected shouldBe DefFlag.Fixed(true)
        val nav = definition.root.children[1].shouldBeInstanceOf<DefNav>()
        nav.selected shouldBe DefFlag.Bound("{ativa}")
        nav.bind shouldBe "categoria"
        warnings.shouldBeEmpty()
        definition.keys shouldContain "categoria"
        definition.keys shouldContain "ativa"
        definition.keys shouldContain "falta"
    }

    "flags resolvem true e false e qualquer outra coisa e falso" {
        val values = mapOf("{sim}" to "true", "{nao}" to "false", "{outro}" to "talvez", "{maiuscula}" to " TRUE ")

        DefFlag.Bound("{sim}").resolve { values.getValue(it) } shouldBe true
        DefFlag.Bound("{nao}").resolve { values.getValue(it) } shouldBe false
        DefFlag.Bound("{outro}").resolve { values.getValue(it) } shouldBe false
        DefFlag.Bound("{maiuscula}").resolve { values.getValue(it) } shouldBe true
        DefFlag.Fixed(true).resolve { error("nao deveria expandir") } shouldBe true
    }

    "slot le item e count dinamicos, name e lore" {
        val (definition, warnings) = DialogFixtures.parse(
            """
            {"theme": "stone", "column": [
              {"slots": [1, 1], "items": [
                {"at": 0, "item": "{item}", "count": "{qtd}", "name": "<yellow>{nome}", "lore": ["<gray>{custo}", "Clique"]}
              ]},
              {"slots": [2, 1], "items": [
                {"at": 1, "item": "minecraft:Diamond_Sword", "count": 3, "name": "Espada", "lore": "linha unica"}
              ]}
            ]}
            """.trimIndent(),
        )

        val dynamic = definition!!.root.children[0].shouldBeInstanceOf<DefSlots>().items.single()
        dynamic shouldBe DefSlotItem(0, "{item}", "{qtd}", "<yellow>{nome}", listOf("<gray>{custo}", "Clique"))
        val fixed = definition.root.children[1].shouldBeInstanceOf<DefSlots>().items.single()
        fixed shouldBe DefSlotItem(1, "minecraft:Diamond_Sword", "3", "Espada", listOf("linha unica"))
        warnings.shouldBeEmpty()
        definition.keys.shouldContainAll("item", "qtd", "nome", "custo")
    }

    "count fixo invalido avisa e vira 1" {
        val (definition, warnings) = DialogFixtures.parse(
            """{"theme": "stone", "column": [{"slots": [1, 1], "items": [{"at": 0, "item": "stone", "count": "muitos"}]}]}""",
        )

        definition!!.root.children.single().shouldBeInstanceOf<DefSlots>().items.single().count shouldBe "1"
        warnings.single() shouldContain "count"
    }

    "repeat no no raiz avisa e e ignorado" {
        val (definition, warnings) = DialogFixtures.parse(
            """{"theme": "stone", "repeat": "l", "column": [{"text": "a"}]}""",
        )

        definition!!.root.children.single().shouldBeInstanceOf<DefText>()
        warnings.single() shouldContain "repeat"
    }

    "chave de lista desconhecida continua so avisando" {
        val (definition, warnings) = DialogFixtures.parse(
            """{"theme": "stone", "column": [{"repeat": "l", "text": "a", "outra": 1}]}""",
        )

        definition!!.root.children.single().shouldBeInstanceOf<DefRepeat>()
        warnings shouldContainExactly listOf("[ui:tela] column#0 > text#0: chave desconhecida 'outra'")
    }
})
