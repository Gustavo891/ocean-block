package one_block.textura.dialog

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import one_block.textura.api.DialogBinds
import one_block.textura.dialog.json.ScreenCompiler
import one_block.textura.dialog.kit.UiUsage
import one_block.textura.dialog.layout.UiLayout

class ProgressTest : StringSpec({

    val json = """
        {"theme": "stone", "column": [
          {"progress": "{valor}", "max": "{maximo}", "track": "ui:trilho", "fill": "ui:cheio", "w": 200}
        ]}
    """.trimIndent()

    fun widths(valor: String, maximo: String): Map<String, Int> {
        val (definition, warnings) = DialogFixtures.parse(json)
        warnings.shouldBeEmpty()
        val binds = DialogBinds().apply {
            text("valor") { valor }
            text("maximo") { maximo }
        }
        val session = DialogSession(definition!!.id, binds, player = DialogFixtures.player())
        val screen = ScreenCompiler(DialogFixtures.kit) { }.compile(definition, DialogFixtures.theme, session)
        return UiLayout(DialogFixtures.kit).layout(screen).images.associate { it.id to it.width }
    }

    "a barra enche na proporcao do valor" {
        widths("2500", "10000") shouldBe mapOf("ui:trilho" to 200, "ui:cheio" to 50)
    }

    "valor zero ou invalido mostra so o trilho" {
        widths("0", "10000") shouldBe mapOf("ui:trilho" to 200)
        widths("abc", "10000") shouldBe mapOf("ui:trilho" to 200)
    }

    "valor acima do maximo para na largura toda" {
        widths("20000", "10000") shouldBe mapOf("ui:trilho" to 200, "ui:cheio" to 200)
    }

    "barra com grow ocupa a largura que sobra e enche na proporcao" {
        val grow = """
            {"theme": "stone", "column": [
              {"progress": "{valor}", "max": "{maximo}", "track": "ui:trilho", "fill": "ui:cheio", "w": "grow"}
            ], "w": 160}
        """.trimIndent()
        val (definition, _) = DialogFixtures.parse(grow)
        val binds = DialogBinds().apply {
            text("valor") { "1" }
            text("maximo") { "4" }
        }
        val session = DialogSession(definition!!.id, binds, player = DialogFixtures.player())
        val screen = ScreenCompiler(DialogFixtures.kit) { }.compile(definition, DialogFixtures.theme, session)
        val placed = UiLayout(DialogFixtures.kit).layout(screen).images.associate { it.id to it.width }

        placed shouldBe mapOf("ui:trilho" to 160, "ui:cheio" to 40)
    }

    "o gerar assa o trilho e o preenchimento mesmo sem valor" {
        val definition = DialogFixtures.parse(json).first!!

        val usage = UiUsage.collect(listOf(definition), mapOf("stone" to DialogFixtures.theme), DialogFixtures.kit, emptySet()) { }

        usage.images shouldContainAll listOf("ui:trilho", "ui:cheio")
    }
})
