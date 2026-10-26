package ocean_block.system.level.levelup

object RewardText {

    private const val SPACE_ADVANCE = 4
    private const val DEFAULT_WIDTH = 5

    private val WIDTHS: Map<Char, Int> = buildMap {
        "!',.:;i|íì".forEach { put(it, 1) }
        "l`".forEach { put(it, 2) }
        "It[]".forEach { put(it, 3) }
        "\"()*<>fk{}".forEach { put(it, 4) }
        "@~".forEach { put(it, 6) }
    }

    fun width(text: String): Int = text.sumOf { if (it == ' ') SPACE_ADVANCE else (WIDTHS[it] ?: DEFAULT_WIDTH) + 1 }
}
