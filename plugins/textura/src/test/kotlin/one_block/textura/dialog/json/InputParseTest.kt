package one_block.textura.dialog.json

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import one_block.textura.api.DialogBinds
import one_block.textura.dialog.DialogFixtures
import one_block.textura.dialog.DialogSession

class InputParseTest : StringSpec({

    "campo de texto e botão de enviar saem do raiz da tela" {
        val (definition, warnings) = DialogFixtures.parse(
            """{"theme": "stone", "input": {"key": "jogador", "label": "Nome do jogador", "max": 16, "w": 200},
               "submit": {"label": "Convidar", "bind": "convidar"}, "column": [{"text": "a"}]}""",
        )

        warnings.shouldBeEmpty()
        definition!!.input shouldBe DefInput("jogador", "Nome do jogador", 16, 200)
        definition.submit shouldBe DefSubmit("Convidar", "convidar")
        definition.keys shouldContainAll listOf("jogador", "convidar")
    }

    "sem input a tela continua sem campo" {
        val (definition, _) = DialogFixtures.parse("""{"theme": "stone", "column": [{"text": "a"}]}""")

        definition!!.input.shouldBeNull()
        definition.submit.shouldBeNull()
    }

    "input sem key avisa e é ignorado, tamanho fora do limite é preso" {
        val (definition, warnings) = DialogFixtures.parse(
            """{"theme": "stone", "input": {"label": "x", "max": 999, "w": 5000}, "column": [{"text": "a"}]}""",
        )

        definition!!.input.shouldBeNull()
        warnings.joinToString() shouldContain "input"

        val (clamped, _) = DialogFixtures.parse(
            """{"theme": "stone", "input": {"key": "k", "max": 999, "w": 5000}, "column": [{"text": "a"}]}""",
        )
        clamped!!.input shouldBe DefInput("k", "", 256, 1024)
    }

    "enviar entrega o texto digitado ao botão do código" {
        var typed: String? = null
        val binds = DialogBinds().apply { button("convidar") { click -> typed = click.input("jogador") } }
        val session = DialogSession("tela", binds, player = DialogFixtures.player())

        session.press("convidar", DialogFixtures.player(), mapOf("jogador" to "Steve"))

        typed shouldBe "Steve"
        session.value("jogador") shouldBe "Steve"
    }
})
