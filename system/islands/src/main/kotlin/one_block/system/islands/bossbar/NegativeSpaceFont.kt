package one_block.system.islands.bossbar

object NegativeSpaceFont {
    private val BASE_CODEPOINT = "󐀀".codePointAt(0) // = 0 pixels

    /**
     * Converte um valor de pixels para o caractere Unicode do NegativeSpaceFont.
     * Ex: pixelsToChar(-10) → '󏿶'
     *     pixelsToChar(0)   → '󐀀'
     */
    fun pixelsToChar(pixels: Int): String {
        return String(Character.toChars(BASE_CODEPOINT + pixels))
    }

    /**
     * Converte múltiplos valores e concatena (útil para offsets compostos).
     * Ex: pixelsToString(-10, -2) → "󏿶󏿾"
     */
    fun pixelsToString(vararg pixels: Int): String {
        return pixels.joinToString("") { pixelsToChar(it) }
    }
}