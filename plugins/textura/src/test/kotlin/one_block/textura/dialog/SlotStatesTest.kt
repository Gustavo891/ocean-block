package one_block.textura.dialog

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import net.kyori.adventure.text.format.NamedTextColor
import one_block.textura.api.DialogBinds
import one_block.textura.dialog.json.ScreenCompiler
import one_block.textura.dialog.layout.LaidOut
import one_block.textura.dialog.layout.UiLayout
import one_block.textura.dialog.theme.SlotsTheme
import one_block.textura.dialog.theme.UiTextStyle

class SlotStatesTest : StringSpec({

    val theme = DialogFixtures.theme.copy(
        styles = DialogFixtures.theme.styles + ("bad" to UiTextStyle(NamedTextColor.RED, false)),
        slots = SlotsTheme("ui:slot", "body", "ui:slot_missing"),
    )

    fun render(json: String, values: Map<String, String>): Pair<LaidOut, List<String>> {
        val (definition, warnings) = DialogFixtures.parse(json)
        val binds = DialogBinds().apply { values.forEach { (key, value) -> text(key) { value } } }
        val session = DialogSession(definition!!.id, binds, player = DialogFixtures.player())
        val screen = ScreenCompiler(DialogFixtures.kit) { }.compile(definition, theme, session)
        return UiLayout(DialogFixtures.kit).layout(screen) to warnings
    }

    "slot com missing verdadeiro usa a arte de falta do tema" {
        val (laid, warnings) = render(
            """
            {"theme": "stone", "column": [{"slots": [3, 1], "items": [
              {"at": 0, "item": "stone", "name": "x", "missing": "{f0}"},
              {"at": 1, "item": "dirt", "name": "x", "missing": "{f1}"}
            ]}]}
            """.trimIndent(),
            mapOf("f0" to "true", "f1" to "false"),
        )

        warnings.shouldBeEmpty()
        laid.images.count { it.id == "ui:slot_missing" } shouldBe 1
        laid.images.count { it.id == "ui:slot" } shouldBe 2
    }

    "hideEmpty nao desenha os slots sem item" {
        val (laid, warnings) = render(
            """
            {"theme": "stone", "column": [{"slots": [4, 1], "hideEmpty": true, "items": [
              {"at": 0, "item": "{i0}", "name": "x"},
              {"at": 1, "item": "{i1}", "name": "x"},
              {"at": 2, "item": "{i2}", "name": "x"}
            ]}]}
            """.trimIndent(),
            mapOf("i0" to "stone", "i1" to "", "i2" to "dirt"),
        )

        warnings.shouldBeEmpty()
        laid.images.count { it.id == "ui:slot" } shouldBe 2
    }

    "hideEmpty sem nenhum item mantem a altura da linha de slots" {
        val json = """
            {"theme": "stone", "column": [
              {"slots": [2, 1], "hideEmpty": true, "items": [{"at": 0, "item": "{i0}", "name": "x"}]},
              {"text": "embaixo"}
            ]}
        """.trimIndent()
        val (cheio, _) = render(json, mapOf("i0" to "stone"))
        val (vazio, _) = render(json, mapOf("i0" to ""))

        vazio.texts.single { it.text == "embaixo" }.y shouldBe cheio.texts.single { it.text == "embaixo" }.y
    }

    "estilo do texto aceita placeholder" {
        val (laid, warnings) = render(
            """{"theme": "stone", "column": [{"text": "Faltam itens", "style": "{estilo}"}]}""",
            mapOf("estilo" to "bad"),
        )

        warnings.shouldBeEmpty()
        laid.texts.single { it.text == "Faltam itens" }.color shouldBe NamedTextColor.RED
    }
})
