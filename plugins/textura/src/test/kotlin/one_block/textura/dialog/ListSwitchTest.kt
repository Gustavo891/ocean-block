package one_block.textura.dialog

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import one_block.textura.api.DialogBinds
import one_block.textura.dialog.json.ScreenCompiler
import one_block.textura.dialog.layout.UiLayout

class ListSwitchTest : StringSpec({

    val json = """
        {"theme": "stone", "column": [
          {"repeat": "ajustes", "perPage": 3, "row": [
            {"text": "{nome}"},
            {"switch": "{ligado}", "bind": "alternar"}
          ]}
        ]}
    """.trimIndent()

    "switch ligado a um campo da lista mostra o estado da entrada e chama o listButton com o índice" {
        val (definition, warnings) = DialogFixtures.parse(json)
        warnings.joinToString() shouldBe ""
        val toggled = mutableListOf<Int>()
        val binds = DialogBinds().apply {
            list("ajustes") { listOf(mapOf("nome" to "A", "ligado" to "true"), mapOf("nome" to "B", "ligado" to "false")) }
            listButton("alternar") { _, index -> toggled += index }
        }
        val session = DialogSession(definition!!.id, binds, player = DialogFixtures.player())
        val laid = UiLayout(DialogFixtures.kit).layout(ScreenCompiler(DialogFixtures.kit) { }.compile(definition, DialogFixtures.theme, session))

        laid.images.map { it.id } shouldContainExactly listOf("ui:on", "ui:off")
        laid.hotspots.mapNotNull { it.action }.forEach { it.run(DialogFixtures.player()) }

        toggled shouldContainExactly listOf(0, 1)
        definition.controlBinds shouldNotContain "alternar"
        definition.defaults.keys shouldNotContain "alternar"
    }
})
