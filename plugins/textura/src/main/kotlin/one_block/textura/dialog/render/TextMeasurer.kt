package one_block.textura.dialog.render

import one_block.textura.dialog.kit.UiKit

class TextMeasurer(private val kit: UiKit) {

    fun width(text: String, bold: Boolean): Int =
        text.codePoints().toArray().sumOf { advance(it, bold) }

    fun fit(text: String, bold: Boolean, max: Int): String {
        if (width(text, bold) <= max) return text

        val ellipsis = width(ELLIPSIS, bold)
        if (ellipsis > max) return ""

        val codePoints = text.codePoints().toArray()
        var used = ellipsis
        var count = 0
        while (count < codePoints.size) {
            val next = advance(codePoints[count], bold)
            if (used + next > max) break
            used += next
            count++
        }
        return String(codePoints, 0, count).trimEnd() + ELLIPSIS
    }

    private fun advance(codePoint: Int, bold: Boolean): Int = kit.charAdvance(codePoint) + if (bold) 1 else 0

    companion object {
        const val ELLIPSIS = "..."
    }
}

class Spaces(private val kit: UiKit) {

    private val steps = (8 downTo 0).map { 1 shl it }

    fun of(amount: Int): String {
        if (amount == 0) return ""

        val sign = if (amount > 0) 1 else -1
        var left = amount * sign
        val out = StringBuilder()

        while (left > 0) {
            val step = steps.first { it <= left }
            out.append(checkNotNull(kit.spaceChar(step * sign)) { "Espaço ${step * sign} ausente no kit" })
            left -= step
        }
        return out.toString()
    }
}
