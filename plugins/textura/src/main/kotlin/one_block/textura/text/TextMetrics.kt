package one_block.textura.text

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import one_block.textura.api.GlyphApi

object TextMetrics {

    fun calculateWidth(text: String): Int {
        if (text.isEmpty()) return 0

        var advance = 0
        val length = text.length

        for (i in 0 until length) {
            val ch = text[i]
            val glyphSize = GlyphApi.sizeOfChar(ch.toString())
            if (glyphSize != null && glyphSize > 0) {
                advance += glyphSize
                continue
            }

            val shift = SpacingFont.shiftAdvance(ch)
            if (shift != null) {
                advance += shift
                continue
            }

            val known = DefaultFontWidths.of(ch.code)
            if (known != null) {
                advance += known
                continue
            }

            advance += when (ch) {
                'i', '!', '\'', '.', ',', ':', ';', '|' -> 2
                'l', '`' -> 3
                't', 'I', '[', ']', '"', '{', '}', '(', ')', '*', ' ' -> 4
                'f', 'k', '<', '>', '+', '-', '=', '?' -> 5
                '@', '~' -> 7
                else -> 6
            }
        }

        return advance
    }

    fun getSpacingComponent(pixels: Int): Component = SpacingFont.component(pixels)

}