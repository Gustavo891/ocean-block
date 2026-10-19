package one_block.textura.dialog

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import one_block.textura.api.DialogBinds
import one_block.textura.dialog.json.ScreenCompiler
import one_block.textura.dialog.layout.UiLayout

class TooltipColorsTest : StringSpec({

    val json = """{"theme": "stone", "column": [{"button": "X", "bind": "b", "tooltip": "{nome}\n{lista}\n\n{acao}"}]}"""

    fun lines(values: Map<String, String>): List<Pair<String, TextColor?>> {
        val definition = DialogFixtures.parse(json).first.shouldNotBeNull()
        val binds = DialogBinds().apply { values.forEach { (key, value) -> text(key) { value } } }
        val session = DialogSession(definition.id, binds, player = DialogFixtures.player())
        val screen = ScreenCompiler(DialogFixtures.kit) { }.compile(definition, DialogFixtures.theme, session)
        val tooltip = UiLayout(DialogFixtures.kit).layout(screen).hotspots.firstNotNullOf { it.tooltip }
        val root = tooltip.asHoverEvent().value() as Component
        return root.children()
            .filterIndexed { index, _ -> index % 2 == 0 }
            .map { PlainTextComponentSerializer.plainText().serialize(it) to it.color() }
    }

    val title = TextColor.color(0xFFD56A)
    val highlight = TextColor.color(0xE6E6E6)
    val success = TextColor.color(0x6FE06F)
    val error = TextColor.color(0xFF6B6B)

    "valores com quebra de linha viram linhas próprias e a cor sai do valor resolvido" {
        val result = lines(mapOf("nome" to "Gancho", "lista" to "∙ 3 Tábuas\n∙ <red>2 Linhas", "acao" to "✔ Pronto!"))

        result.map { it.first } shouldContainExactly listOf("Gancho", "∙ 3 Tábuas", "∙ <red>2 Linhas", "", "✔ Pronto!")
        result.map { it.second }.let { listOf(it[0], it[1], it[2], it[4]) } shouldContainExactly listOf(title, highlight, highlight, success)
    }

    "ação com marca de erro fica vermelha" {
        lines(mapOf("nome" to "A", "lista" to "b", "acao" to "⌀ Falta: 1 Linha")).last().second shouldBe error
        lines(mapOf("nome" to "A", "lista" to "b", "acao" to "✖ Bloqueado")).last().second shouldBe error
    }
})
