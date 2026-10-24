package one_block.textura.dialog

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import one_block.textura.api.DialogBinds
import one_block.textura.dialog.json.ScreenCompiler
import one_block.textura.dialog.layout.ImageVariant
import one_block.textura.dialog.layout.UiLayout
import one_block.textura.dialog.theme.UiTheme

class IconFrameTest : StringSpec({

    fun images(json: String, theme: UiTheme, warnings: MutableList<String>) = run {
        val (definition, parseWarnings) = DialogFixtures.parse(json)
        parseWarnings.shouldBeEmpty()
        val session = DialogSession(definition!!.id, DialogBinds(), player = DialogFixtures.player())
        val screen = ScreenCompiler(DialogFixtures.kit) { warnings += it }.compile(definition, theme, session)
        UiLayout(DialogFixtures.kit).layout(screen).images
    }

    val json = """{"theme": "stone", "column": [{"icon": "ui:gerador", "size": 32, "frame": true}]}"""

    "frame desenha a surface do tema em volta do icone com 4 px de folga" {
        val theme = DialogFixtures.theme.copy(surfaces = DialogFixtures.theme.surfaces + ("frame" to "ui:stone/well"))
        val placed = images(json, theme, mutableListOf())

        placed.map { it.id } shouldContainExactly listOf("ui:stone/well", "ui:gerador")
        val frame = placed.first()
        (frame.variant == ImageVariant.Surface) shouldBe true
        (frame.width to frame.height) shouldBe (40 to 40)
        val icon = placed.last()
        (icon.x - frame.x to icon.y - frame.y) shouldBe (4 to 4)
    }

    "tema sem surface frame avisa e desenha so o icone" {
        val warnings = mutableListOf<String>()
        images(json, DialogFixtures.theme, warnings).map { it.id } shouldContainExactly listOf("ui:gerador")
        warnings.size shouldBe 1
    }
})
