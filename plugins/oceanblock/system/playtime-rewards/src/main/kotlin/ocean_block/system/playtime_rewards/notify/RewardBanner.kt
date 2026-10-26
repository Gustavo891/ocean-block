package ocean_block.system.playtime_rewards.notify

object RewardBanner {

    const val FRAMES = 3
    const val LINE_1 = "Recompensa liberada!"
    const val LINE_2 = "Use /recompensas para resgatar"

    private const val GLYPH = "tempo_faixa_"
    private const val GENERIC = "generico"
    private const val JOIN = "<shift:-1>"

    fun textPiece(minutes: Int, exists: (String) -> Boolean): String =
        "$GLYPH$minutes".takeIf(exists) ?: "$GLYPH$GENERIC"

    fun title(minutes: Int, frame: Int, exists: (String) -> Boolean): String =
        "<white>" + listOf("${GLYPH}esq_$frame", textPiece(minutes, exists), "${GLYPH}dir_$frame")
            .joinToString(JOIN) { "<glyph:$it>" }

    fun lines(): List<String> = listOf(centered("gold", LINE_1), centered("green", LINE_2))

    private fun centered(color: String, text: String): String = "<shift:-${LineText.width(text) / 2}><$color>$text"
}
