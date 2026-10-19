package one_block.textura.notify.layout

internal object PlaquePieces {

    const val CAP_WIDTH = 1
    val BODY_WIDTHS: List<Int> = listOf(128, 64, 32, 16, 8, 4, 2, 1)

    fun body(width: Int): List<Int> {
        var remaining = (width - 2 * CAP_WIDTH).coerceAtLeast(0)
        val pieces = mutableListOf<Int>()
        for (piece in BODY_WIDTHS) {
            while (remaining >= piece) {
                pieces += piece
                remaining -= piece
            }
        }
        return pieces
    }
}
