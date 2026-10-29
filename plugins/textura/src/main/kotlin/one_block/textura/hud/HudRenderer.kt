package one_block.textura.hud

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import one_block.textura.api.TexturaTags
import one_block.textura.text.TextMetrics

object HudRenderer {

    private val miniMessage = TexturaTags.miniMessage
    private val plainText = PlainTextComponentSerializer.plainText()

    fun render(content: HudContent): Component = render(content, null)

    fun render(content: HudContent, secondary: HudSecondary?): Component {
        val glyphs = HudBackground.resolve(content.background)
        val main = part(content.text, glyphs)

        if (secondary == null) return main.component
        val side = part(secondary.text, HudBackground.resolve())

        return when (secondary.side) {
            HudSide.RIGHT -> composeRight(main, side, secondary.gap)
            HudSide.LEFT -> composeLeft(main, side, secondary.gap)
        }
    }

    /**
     * Um bloco de HUD já montado (fundo + texto).
     *
     * [width] é a largura desenhada, da borda esquerda à direita do fundo; [cursor]
     * é o avanço total do componente. O bloco termina com um espaçamento que iguala
     * os dois, para que o avanço que o cliente mede — e usa para centralizar a
     * bossbar — seja exatamente a largura do fundo.
     */
    private data class Part(val component: Component, val width: Int, val cursor: Int)

    private fun part(text: String, glyphs: HudBackground.Glyphs): Part {
        val textComp = HudRaise.mark(miniMessage.deserialize("<font:minecraft:bossbar>$text</font>"))

        val plain = plainText.serialize(miniMessage.deserialize(text))
        val textPixels = TextMetrics.calculateWidth(plain)

        if (!glyphs.available) {
            return Part(noShadow().append(textComp), textPixels, textPixels)
        }

        val segments = HudLayout.segmentsFor(textPixels, glyphs.bodyW) + 2
        val width = HudLayout.backgroundWidth(segments, glyphs.leftW, glyphs.bodyW, glyphs.rightW)

        // Onde o texto começa e onde o cursor está quando o fundo termina. O cursor
        // passa 1px da borda: o último glyph avança largura+1 e não leva o -1 de
        // emenda que os outros levam.
        val textStart = HudBackground.textStart(width, textPixels, glyphs)
        val afterBackground = HudLayout.cursorAfterBackground(width)

        val component = Component.empty()
            .append(HudBackground.build(segments, glyphs))
            .append(TextMetrics.getSpacingComponent(textStart - afterBackground))
            .append(textComp)
            .append(TextMetrics.getSpacingComponent(width - textStart - textPixels))

        return Part(component, width, width)
    }

    /**
     * Secundária depois da principal. O `lead` leva o cursor da borda direita da
     * principal até onde a secundária começa; o espaço negativo final devolve o
     * cursor e mantém o avanço total igual ao da principal sozinha — é o que
     * preserva a centralização dela.
     */
    private fun composeRight(main: Part, side: Part, gap: Int): Component {
        val lead = (main.width - main.cursor) + gap
        return Component.empty()
            .append(main.component)
            .append(TextMetrics.getSpacingComponent(lead))
            .append(side.component)
            .append(TextMetrics.getSpacingComponent(-(lead + side.cursor)))
    }

    /**
     * Secundária antes da principal: recua o cursor pela largura dela mais o
     * espaçamento, desenha, e volta ao ponto de partida da principal. A soma dos
     * espaçamentos com o avanço da secundária também é zero.
     */
    private fun composeLeft(main: Part, side: Part, gap: Int): Component {
        val lead = -(gap + side.width)
        return Component.empty()
            .append(TextMetrics.getSpacingComponent(lead))
            .append(side.component)
            .append(TextMetrics.getSpacingComponent(-lead - side.cursor))
            .append(main.component)
    }

    /** Raiz vazia sem sombra; todos os filhos herdam o `shadow_color` transparente. */
    private fun noShadow(): Component = Component.empty().shadowColor(ShadowColor.none())
}
