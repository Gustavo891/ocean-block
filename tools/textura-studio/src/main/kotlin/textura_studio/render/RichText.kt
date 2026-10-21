package textura_studio.render

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import one_block.textura.api.TexturaTags

/** Um trecho de texto já com cor e negrito resolvidos. */
data class TextRun(val text: String, val rgb: Int, val bold: Boolean)

/**
 * Achata MiniMessage em trechos. `<lang:...>` é resolvido no dicionário (pt_br), como
 * as notificações fazem no servidor; `legacy` troca cada cor pela mais próxima das 16
 * cores de chat, que é o que o shader das notificações consegue pintar.
 */
class RichText(private val lang: Map<String, String>, private val legacy: Boolean) {

    fun runs(miniMessage: String): List<TextRun> {
        val component = runCatching { TexturaTags.miniMessage.deserialize(miniMessage) }.getOrElse { Component.text(miniMessage) }
        val out = mutableListOf<TextRun>()
        visit(component, null, false, out)
        return out.filter { it.text.isNotEmpty() }
    }

    private fun visit(component: Component, parentColor: TextColor?, parentBold: Boolean, out: MutableList<TextRun>) {
        val color = component.color() ?: parentColor
        val bold = when (component.decoration(TextDecoration.BOLD)) {
            TextDecoration.State.TRUE -> true
            TextDecoration.State.FALSE -> false
            TextDecoration.State.NOT_SET -> parentBold
        }
        val rgb = color?.value()?.let { if (legacy) nearest(it) else it } ?: WHITE
        when (component) {
            is TextComponent -> out += TextRun(component.content(), rgb, bold)
            is TranslatableComponent -> out += TextRun(translate(component), rgb, bold)
            else -> Unit
        }
        component.children().forEach { visit(it, color, bold, out) }
    }

    private fun translate(component: TranslatableComponent): String {
        val pattern = lang[component.key()] ?: component.fallback() ?: component.key()
        val args = component.arguments().map { argument ->
            val value = argument.asComponent()
            if (value is TextComponent) value.content() else value.toString()
        }
        var index = 0
        return PLACEHOLDER.replace(pattern) { match ->
            val explicit = match.groupValues[1].toIntOrNull()
            val at = if (explicit != null) explicit - 1 else index++
            args.getOrElse(at) { "" }
        }
    }

    companion object {
        const val WHITE = 0xFFFFFF
        private val PLACEHOLDER = Regex("""%(?:(\d+)\$)?s""")

        /** As 16 cores de `§0`..`§f`, na ordem da paleta do shader. */
        val PALETTE = intArrayOf(
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF,
        )

        fun nearest(rgb: Int): Int = PALETTE.minBy { candidate ->
            val dr = (candidate shr 16 and 0xFF) - (rgb shr 16 and 0xFF)
            val dg = (candidate shr 8 and 0xFF) - (rgb shr 8 and 0xFF)
            val db = (candidate and 0xFF) - (rgb and 0xFF)
            dr * dr + dg * dg + db * db
        }
    }
}
