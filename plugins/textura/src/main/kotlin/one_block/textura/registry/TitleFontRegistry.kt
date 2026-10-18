package one_block.textura.registry

/**
 * De qual JSON de fonte a fonte de título nasce.
 *
 * [baseAscent] é o ascent do provider principal dentro desse JSON — é o valor que o
 * `y` do título passa a significar. Com ele o `y` quer dizer a mesma coisa em
 * qualquer fonte: o ascent efetivo do provider principal.
 */
data class TitleFontSource(
    val path: String = TitleFontRegistry.DEFAULT_SOURCE,
    val baseAscent: Int = TitleFontRegistry.BASE_ASCENT,
) {

    val slug: String get() = path.substringAfterLast('/').substringBeforeLast('.')

    val isDefault: Boolean
        get() = path == TitleFontRegistry.DEFAULT_SOURCE && baseAscent == TitleFontRegistry.BASE_ASCENT
}

/**
 * Uma fonte de título pedida em runtime: um ascent com um tamanho de glyph.
 *
 * Na fonte padrão [height] é proporcional à vanilla (8 = tamanho normal). Numa
 * fonte custom ele é a altura literal dos glyphs; `null` mantém a do JSON.
 */
data class TitleFont(
    val ascent: Int,
    val height: Int?,
    val source: TitleFontSource = TitleFontSource(),
) {

    val id: String get() = TitleFontRegistry.fontId(ascent, height, source)
}

class TitleFontRegistry {

    // Set of all title fonts requested during runtime
    private val requested = java.util.concurrent.ConcurrentHashMap.newKeySet<TitleFont>()

    fun register(y: Int, height: Int?, source: TitleFontSource = TitleFontSource()) {
        requested.add(TitleFont(y, height, source))
    }

    fun getRequested(): Set<TitleFont> {
        return requested.toSet()
    }

    fun clear() {
        requested.clear()
    }

    companion object {

        /** Altura padrão dos glyphs da fonte vanilla. */
        const val BASE_HEIGHT = 8

        /** Ascent do provider ascii da fonte vanilla. */
        const val BASE_ASCENT = 7

        /** JSON de fonte usado quando nenhum outro é pedido. */
        const val DEFAULT_SOURCE = "custom.json"

        /**
         * Id da fonte gerada. Mantém `font-ui-<ascent>` no caso padrão para não
         * quebrar os títulos já existentes.
         */
        fun fontId(ascent: Int, height: Int?, source: TitleFontSource = TitleFontSource()): String = when {
            !source.isDefault -> "font-ui-${source.slug}-$ascent" + (height?.let { "-h$it" } ?: "")
            height == null || height == BASE_HEIGHT -> "font-ui-$ascent"
            else -> "font-ui-$ascent-h$height"
        }
    }
}
