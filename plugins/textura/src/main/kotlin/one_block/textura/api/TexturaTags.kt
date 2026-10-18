package one_block.textura.api

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.Context
import net.kyori.adventure.key.Key
import one_block.textura.text.TextMetrics

/**
 * Tags MiniMessage do Textura:
 *
 * - `<glyph:id>` — char do glyph na fonte padrao (`minecraft:default`).
 * - `<glyph:id:fonte>` — char do glyph na fonte nomeada (ex.: `<glyph:espada:bossbar>`).
 * - `<shift:n>` — deslocamento horizontal de `n` pixels, positivo ou negativo.
 *
 * Ambas sao self-closing e nunca lancam: id ou fonte inexistente vira componente vazio,
 * para que um glyph faltando nao quebre um titulo de GUI inteiro.
 */
object TexturaTags {

    private val DEFAULT_FONT = Key.key("minecraft", "default")

    val glyph: TagResolver = TagResolver.resolver("glyph") { args: ArgumentQueue, _: Context ->
        val id = args.popOr("<glyph> exige um id de glyph").value()
        val font = if (args.hasNext()) args.pop().value() else null
        Tag.selfClosingInserting(glyphComponent(id, font))
    }

    val shift: TagResolver = TagResolver.resolver("shift") { args: ArgumentQueue, _: Context ->
        val pixels = args.popOr("<shift> exige um numero de pixels").asInt().orElse(0)
        Tag.selfClosingInserting(TextMetrics.getSpacingComponent(pixels))
    }

    /** Os dois resolvers do Textura somados aos padroes do MiniMessage. */
    val resolver: TagResolver = TagResolver.builder()
        .resolver(TagResolver.standard())
        .resolver(glyph)
        .resolver(shift)
        .build()

    /** Instancia MiniMessage que conhece `<glyph>` e `<shift>`. */
    val miniMessage: MiniMessage = MiniMessage.builder().tags(resolver).build()

    private fun glyphComponent(id: String, font: String?): Component {
        if (font == null) {
            val char = GlyphApi.getChar(id) ?: return Component.empty()
            return Component.text(char).style(Style.style().font(DEFAULT_FONT).build())
        }

        val char = GlyphApi.getChar(id, font) ?: return Component.empty()
        val key = GlyphApi.getFontKey(id, font) ?: return Component.empty()
        return Component.text(char).style(Style.style().font(key).build())
    }
}
