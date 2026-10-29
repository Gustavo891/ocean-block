package one_block.textura.text

object DefaultFontWidths {

    private const val RESOURCE = "/fonts/minecraft_default_widths.txt"

    private val widths: Map<Int, Int> by lazy { load() }

    fun of(codepoint: Int): Int? = widths[codepoint]

    private fun load(): Map<Int, Int> {
        val stream = DefaultFontWidths::class.java.getResourceAsStream(RESOURCE) ?: return emptyMap()
        return stream.bufferedReader().useLines { lines ->
            lines.mapNotNull { line ->
                val parts = line.trim().split(' ')
                val codepoint = parts.getOrNull(0)?.toIntOrNull(16)
                val width = parts.getOrNull(1)?.toDoubleOrNull()?.toInt()
                if (codepoint == null || width == null) null else codepoint to width
            }.toMap()
        }
    }
}
