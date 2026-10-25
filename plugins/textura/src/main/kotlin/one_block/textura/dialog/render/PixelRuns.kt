package one_block.textura.dialog.render

data class PixelRun(val x: Int, val width: Int, val rgb: Int)

object PixelRuns {

    private const val OPAQUE = 128
    private const val MARKER_RED = 253

    fun of(row: IntArray): List<PixelRun> {
        val runs = mutableListOf<PixelRun>()
        var x = 0
        while (x < row.size) {
            val argb = row[x]
            if ((argb ushr 24) < OPAQUE) {
                x++
                continue
            }
            val rgb = safe(argb and 0xFFFFFF)
            var end = x + 1
            while (end < row.size && (row[end] ushr 24) >= OPAQUE && safe(row[end] and 0xFFFFFF) == rgb) end++
            runs += PixelRun(x, end - x, rgb)
            x = end
        }
        return runs
    }

    fun pieces(run: PixelRun, maxWidth: Int): List<PixelRun> {
        val pieces = mutableListOf<PixelRun>()
        var x = run.x
        var left = run.width
        while (left > 0) {
            var size = Integer.highestOneBit(left).coerceAtMost(Integer.highestOneBit(maxWidth))
            if (size < 1) size = 1
            pieces += PixelRun(x, size, run.rgb)
            x += size
            left -= size
        }
        return pieces
    }

    private fun safe(rgb: Int): Int {
        val red = rgb shr 16
        return if (red >= MARKER_RED && red < 0xFF) (rgb and 0xFFFF) or 0xFF0000 else rgb
    }
}
