package one_block.textura.dialog

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.event.ClickEvent
import one_block.textura.api.DialogBinds
import one_block.textura.dialog.json.ScreenCompiler
import one_block.textura.dialog.kit.UiFonts
import one_block.textura.dialog.kit.UiKit
import one_block.textura.dialog.layout.UiLayout
import one_block.textura.dialog.render.UiRenderer
import one_block.textura.dialog.theme.UiTextStyle

class TextFontStyleTest : StringSpec({

    val kit = object : UiKit by DialogFixtures.kit {
        override fun charAdvance(codePoint: Int, font: String?): Int = if (font == "pequena") 4 else 6
        override fun textFont(offset: Int, font: String?): Key = UiFonts.text(offset, font)
        override fun spaceChar(amount: Int): String = "s"
    }
    val theme = DialogFixtures.theme.copy(
        styles = DialogFixtures.theme.styles + ("caixa" to UiTextStyle(null, false, font = "pequena")),
    )

    fun laid(json: String) = DialogFixtures.parse(json).first!!.let { definition ->
        val session = DialogSession(definition.id, DialogBinds(), player = DialogFixtures.player())
        UiLayout(kit).layout(ScreenCompiler(kit) { }.compile(definition, theme, session))
    }

    "estilo com fonte mede pelas larguras dessa fonte" {
        val laid = laid("""{"theme": "stone", "row": [{"text": "ABC", "style": "caixa"}, {"text": "ABC"}]}""")

        laid.texts.map { it.font } shouldBe listOf("pequena", null)
        laid.texts[1].x shouldBe 12
    }

    "o texto com fonte usa a fonte própria em cada deslocamento" {
        val lines = UiRenderer(kit, { ClickEvent.runCommand("/x") }) { }
            .render(laid("""{"theme": "stone", "column": [{"text": "OI", "style": "caixa"}]}"""))

        lines[0].toString() shouldContain "ui/text_pequena_0"
        lines[0].toString() shouldNotContain "ui/text_0"
    }

    "fonte sem nome usa a fonte de texto padrão" {
        UiFonts.text(3, null) shouldBe UiFonts.text(3)
        UiFonts.text(3, "pequena") shouldBe Key.key("oneblock", "ui/text_pequena_3")
    }
})
