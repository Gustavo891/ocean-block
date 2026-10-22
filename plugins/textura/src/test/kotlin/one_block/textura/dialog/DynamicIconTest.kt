package one_block.textura.dialog

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import one_block.textura.api.DialogBinds
import one_block.textura.dialog.json.ScreenCompiler
import one_block.textura.dialog.kit.IconSize
import one_block.textura.dialog.kit.UiUsage
import one_block.textura.dialog.layout.ImageVariant
import one_block.textura.dialog.layout.UiLayout

class DynamicIconTest : StringSpec({

    val json = """
        {"theme": "stone", "column": [
          {"grid": [{"repeat": "receitas", "perPage": 2, "icon": "item:{item}", "size": 24}], "columns": 2}
        ]}
    """.trimIndent()

    "icone com placeholder usa o valor da entrada no tamanho pedido" {
        val (definition, warnings) = DialogFixtures.parse(json)
        val binds = DialogBinds().apply { list("receitas") { listOf(mapOf("item" to "composter"), mapOf("item" to "loom")) } }
        val session = DialogSession(definition!!.id, binds, player = DialogFixtures.player())
        val screen = ScreenCompiler(DialogFixtures.kit) { }.compile(definition, DialogFixtures.theme, session)
        val icons = UiLayout(DialogFixtures.kit).layout(screen).images.filter { it.variant is ImageVariant.Icon }

        warnings.shouldBeEmpty()
        icons.map { it.id } shouldBe listOf("item:composter", "item:loom")
        icons.map { (it.variant as ImageVariant.Icon).size }.toSet() shouldBe setOf(24)
    }

    "o gerar assa os itens registrados no tamanho do icone dinamico" {
        val definition = DialogFixtures.parse(json).first!!

        val usage = UiUsage.collect(listOf(definition), mapOf("stone" to DialogFixtures.theme), DialogFixtures.kit, setOf("composter")) { }

        usage.icons shouldContain IconSize("item:composter", 24)
    }
})
