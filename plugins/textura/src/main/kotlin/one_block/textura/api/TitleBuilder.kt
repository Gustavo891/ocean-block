package one_block.textura.api

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import one_block.textura.registry.TitleFontRegistry
import one_block.textura.registry.TitleFontSource
import one_block.textura.text.TextMetrics

class TitleBuilder(private val titleFontRegistry: TitleFontRegistry) {

    private val components = mutableListOf<Component>()
    
    // Position of the "cursor" in pixels (X axis)
    private var currentX = 0

    /**
     * Escreve [text] na posição [x] (px, a partir da origem do título) usando a fonte
     * de ascent [y]. [height] é a altura final dos glyphs — 8 é o tamanho padrão do
     * Minecraft, valores menores encolhem o texto proporcionalmente.
     */
    fun addText(text: String, x: Int, y: Int): TitleBuilder = addText(text, x, y, TitleFontRegistry.BASE_HEIGHT)

    fun addText(text: String, x: Int, y: Int, height: Int): TitleBuilder {
        titleFontRegistry.register(y, height)

        val offsetX = x - currentX

        if (offsetX != 0) {
            components.add(getSpacingComponent(offsetX))
        }

        val textWidth = calculateWidth(text, height)
        val fontKey = Key.key("minecraft", TitleFontRegistry.fontId(y, height))

        components.add(Component.text(text).style(Style.style().font(fontKey).build()))

        // 4. Update current cursor X position
        currentX = x + textWidth

        return this
    }

    /**
     * Escreve [text] com uma fonte própria em vez da vanilla.
     *
     * [charWidth] é o avanço de cada caractere em pixels e [letterSpacing] o shift
     * aplicado entre eles (negativo aproxima). A largura total vira
     * `n * charWidth + (n - 1) * letterSpacing`, que é o que move o cursor.
     *
     * [height] força a altura dos glyphs; `null` mantém a do JSON. O avanço que o
     * Minecraft aplica é `arredonda(opacos * height / alturaDaCelula) + 1` — é dele
     * que [charWidth] tem que sair.
     */
    fun addText(text: String, x: Int, y: Int, source: TitleFontSource, charWidth: Int): TitleBuilder =
        addText(text, x, y, source, charWidth, 0, null)

    fun addText(text: String, x: Int, y: Int, source: TitleFontSource, charWidth: Int, letterSpacing: Int): TitleBuilder =
        addText(text, x, y, source, charWidth, letterSpacing, null)

    fun addText(
        text: String,
        x: Int,
        y: Int,
        source: TitleFontSource,
        charWidth: Int,
        letterSpacing: Int,
        height: Int?,
    ): TitleBuilder {
        if (text.isEmpty()) return this

        titleFontRegistry.register(y, height, source)

        val offsetX = x - currentX
        if (offsetX != 0) {
            components.add(getSpacingComponent(offsetX))
        }

        val style = Style.style()
            .font(Key.key("minecraft", TitleFontRegistry.fontId(y, height, source)))
            .build()

        for ((index, ch) in text.withIndex()) {
            if (index > 0 && letterSpacing != 0) {
                components.add(getSpacingComponent(letterSpacing))
            }
            components.add(Component.text(ch.toString()).style(style))
        }

        currentX = x + text.length * charWidth + (text.length - 1) * letterSpacing

        return this
    }

    /**
     * Escreve [text] cru numa fonte já existente no pack — glyphs, por exemplo.
     *
     * [width] é o avanço total que o texto ocupa; num glyph ele é a largura em
     * pixels da textura mais 1. Nada é registrado no [titleFontRegistry]: a fonte
     * tem que existir por conta própria.
     */
    fun addRaw(text: String, x: Int, font: Key, width: Int): TitleBuilder {
        if (text.isEmpty()) return this

        val offsetX = x - currentX
        if (offsetX != 0) {
            components.add(getSpacingComponent(offsetX))
        }

        components.add(Component.text(text).style(Style.style().font(font).build()))
        currentX = x + width

        return this
    }

    /**
     * Largura de [text] na altura [height]. O avanço de cada glyph é
     * `arredonda(largura * escala) + 1`, então o espaçamento de 1px não encolhe.
     */
    private fun calculateWidth(text: String, height: Int): Int {
        if (height == TitleFontRegistry.BASE_HEIGHT) return TextMetrics.calculateWidth(text)

        var advance = 0
        for (ch in text) {
            val base = TextMetrics.calculateWidth(ch.toString())
            if (base <= 0) continue
            advance += Math.round((base - 1) * height.toFloat() / TitleFontRegistry.BASE_HEIGHT) + 1
        }
        return advance
    }


    fun build(): Component {
        var finalComponent = Component.empty()
        for (comp in components) {
            finalComponent = finalComponent.append(comp)
        }
        return finalComponent
    }

    private fun getSpacingComponent(pixels: Int): Component = one_block.textura.text.SpacingFont.component(pixels)

}
