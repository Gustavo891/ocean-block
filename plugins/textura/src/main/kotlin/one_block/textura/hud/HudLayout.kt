package one_block.textura.hud

import kotlin.math.ceil

object HudLayout {

    fun segmentsFor(textPixels: Int, bodyWidth: Int): Int {
        if (bodyWidth <= 0) return 2
        return (ceil(textPixels / bodyWidth.toDouble()).toInt()).coerceAtLeast(2)
    }

    fun backgroundWidth(segments: Int, leftW: Int, bodyW: Int, rightW: Int): Int {
        if (segments <= 2) return leftW + rightW
        return leftW + rightW + bodyW * (segments - 2)
    }

    fun cursorAfterBackground(backgroundWidth: Int): Int = backgroundWidth + 1
}
