package one_block.textura.pack

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import net.kyori.adventure.key.Key
import team.unnamed.creative.base.Writable
import team.unnamed.creative.font.BitMapFontProvider
import team.unnamed.creative.font.Font

class PackBuilderSheetTest : StringSpec({

    val textureKey = Key.key("oneblock", "icons/mouse_clicks.png")
    val texture = Writable.stringUtf8("png-falso")

    "folha vira um provider unico com todos os chars" {
        val builder = PackBuilder()
        builder.addGlyphSheet(
            textureKey = textureKey,
            texture = texture,
            chars = listOf("a", "b", "c"),
            height = 8,
            ascent = 5
        )

        val providers = builder.pack.font(Font.MINECRAFT_DEFAULT)!!.providers()
        providers shouldHaveSize 1

        val bitmap = providers.first() as BitMapFontProvider
        bitmap.characters() shouldContainExactly listOf("a", "b", "c")
        bitmap.file() shouldBe textureKey
        bitmap.height() shouldBe 8
        bitmap.ascent() shouldBe 5
    }

    "a textura da folha entra uma vez so mesmo com duas fontes" {
        val builder = PackBuilder()
        builder.addGlyphSheet(textureKey, texture, listOf("a", "b"), 8, 5, Font.MINECRAFT_DEFAULT)
        builder.addGlyphSheet(textureKey, texture, listOf("a", "b"), 10, 3, Key.key("minecraft", "bossbar"))

        builder.pack.textures() shouldHaveSize 1
        builder.pack.font(Font.MINECRAFT_DEFAULT)!!.providers() shouldHaveSize 1
        builder.pack.font(Key.key("minecraft", "bossbar"))!!.providers() shouldHaveSize 1
    }

    "glyph simples continua sendo um provider de um char" {
        val builder = PackBuilder()
        builder.addGlyph(textureKey, texture, "z", 8, 5)

        val bitmap = builder.pack.font(Font.MINECRAFT_DEFAULT)!!.providers().first() as BitMapFontProvider
        bitmap.characters() shouldContainExactly listOf("z")
    }
})
