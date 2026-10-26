package ocean_block.system.level.levelup

object SmallText {

    private const val SCALE = 0.5
    private const val SPACE_ADVANCE = 2
    private const val DEFAULT_WIDTH = 5

    private val WIDTHS: Map<Char, Int> = buildMap {
        "!',.:;i|íì".forEach { put(it, 1) }
        "l`".forEach { put(it, 2) }
        "It[]".forEach { put(it, 3) }
        "\"()*<>fk{}".forEach { put(it, 4) }
        "@~".forEach { put(it, 6) }
    }

    fun width(text: String): Int = text.sumOf(::advance)

    private fun advance(char: Char): Int {
        if (char == ' ') return SPACE_ADVANCE
        val width = WIDTHS[char] ?: DEFAULT_WIDTH
        return (0.5 + width * SCALE).toInt() + 1
    }
}
