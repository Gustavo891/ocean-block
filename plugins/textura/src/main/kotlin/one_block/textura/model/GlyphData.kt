package one_block.textura.model

import net.kyori.adventure.key.Key

/**
 * Uma fonte nomeada em que o glyph também é registrado (ex.: `bossbar`), com
 * seu próprio deslocamento/altura e, opcionalmente, um unicode distinto do global.
 *
 * Os valores aqui já vêm resolvidos: quando o yml não define `ascent`/`height`
 * da fonte, o [one_block.textura.registry.GlyphRegistry] copia os
 * globais; quando não define `unicode-specify`, herda o unicode global.
 */
data class GlyphFont(
    val name: String,
    val ascent: Int,
    val height: Int,
    val unicode: String,
    /** Namespace da fonte em que o glyph entra (ex.: `oneblock` para `oneblock:gui`). */
    val namespace: String = FontData.DEFAULT_NAMESPACE
) {
    /** Caractere renderizável desta fonte (resolve a forma `\uXXXX`). */
    val char: String get() = GlyphData.toChar(unicode)

    /** Chave da fonte no pack: `<namespace>:<name>`. */
    val fontKey: Key get() = Key.key(namespace, name)
}

data class GlyphData(
    val key: String,
    val id: String,
    val caminho: String,
    var unicode: String,
    /** Deslocamento vertical do glyph na fonte global. Usa [DEFAULT_ASCENT] quando não configurado. */
    val ascent: Int = DEFAULT_ASCENT,
    /** Altura de renderização do glyph na fonte global. Usa [DEFAULT_HEIGHT] quando não configurado. */
    val height: Int = DEFAULT_HEIGHT,
    /** Largura do glyph em pixels, usada para medir o avanço do texto em [one_block.textura.text.TextMetrics]. */
    val size: Int = DEFAULT_SIZE,
    /** Quando `true`, o glyph também é gravado na fonte `minecraft:default` (chat/textos). */
    val default: Boolean = true,
    /** Se deve ser exibido com o char visível no comando /textura glyphs. */
    val showInGui: Boolean = true,
    /** Fontes nomeadas adicionais em que o glyph é registrado, indexadas pelo nome. */
    val fonts: Map<String, GlyphFont> = emptyMap(),
    /** Chave da folha no yml quando o glyph é uma fatia de sprite-sheet, `null` quando não é. */
    val sheetKey: String? = null,
    /** Linha da fatia dentro da folha, 0-based. Sempre `0` em glyph simples. */
    val sheetIndex: Int = 0
) {
    /** Converte o [unicode] (ex.: `` ou um caractere literal) no char renderizável. */
    val char: String
        get() = toChar(unicode)

    companion object {
        const val DEFAULT_ASCENT = 8
        const val DEFAULT_HEIGHT = 8
        const val DEFAULT_SIZE = 8

        /** Resolve a forma `\uXXXX` (ou um caractere literal) para o char renderizável. */
        fun toChar(unicode: String): String =
            if (unicode.startsWith("\\u")) {
                unicode.substring(2).toInt(16).toChar().toString()
            } else {
                unicode
            }
    }
}
