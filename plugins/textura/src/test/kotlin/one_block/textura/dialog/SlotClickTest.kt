package one_block.textura.dialog

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import one_block.textura.api.DialogBinds
import one_block.textura.dialog.json.ScreenCompiler
import one_block.textura.dialog.layout.UiLayout

class SlotClickTest : StringSpec({

    val json = """
        {"theme": "stone", "column": [
          {"grid": [{"repeat": "inventario", "slots": [1, 1], "bind": "colocar",
                     "items": [{"at": 0, "item": "{item}", "count": "{qtd}", "name": "{item}"}]}], "columns": 3}
        ]}
    """.trimIndent()

    "slot com bind numa lista chama listButton com o indice e slot vazio nao clica" {
        val (definition, warnings) = DialogFixtures.parse(json)
        warnings.shouldBeEmpty()
        val clicked = mutableListOf<Int>()
        val binds = DialogBinds().apply {
            list("inventario") { listOf(mapOf("item" to "coal", "qtd" to "3"), mapOf("item" to "", "qtd" to "1"), mapOf("item" to "raw_iron", "qtd" to "9")) }
            listButton("colocar") { _, index -> clicked += index }
        }
        val session = DialogSession(definition!!.id, binds, player = DialogFixtures.player())
        val screen = ScreenCompiler(DialogFixtures.kit) { }.compile(definition, DialogFixtures.theme, session)

        UiLayout(DialogFixtures.kit).layout(screen).hotspots.mapNotNull { it.action }.forEach { it.run(DialogFixtures.player()) }

        clicked shouldContainExactly listOf(0, 2)
    }
})
