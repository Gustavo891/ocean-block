package one_block.textura.dialog.render

import one_block.textura.dialog.kit.UiKit

class TextMeasurer(private val kit: UiKit) {

    fun width(text: String, bold: Boolean, font: String? = null): Int =
        text.codePoints().toArray().sumOf { advance(it, bold, font) }

    fun fit(text: String, bold: Boolean, max: Int, font: String? = null): String {
        if (width(text, bold, font) <= max) return text

        val ellipsis = width(ELLIPSIS, bold, font)
        if (ellipsis > max) return ""

        val codePoints = text.codePoints().toArray()
        var used = ellipsis
        var count = 0
        while (count < codePoints.size) {
            val next = advance(codePoints[count], bold, font)
            if (used + next > max) break
            used += next
            count++
        }
        return String(codePoints, 0, count).trimEnd() + ELLIPSIS
    }

    private fun advance(codePoint: Int, bold: Boolean, font: String?): Int = kit.charAdvance(codePoint, font) + if (bold) 1 else 0

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
