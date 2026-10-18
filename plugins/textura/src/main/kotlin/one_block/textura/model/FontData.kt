package one_block.textura.model

import net.kyori.adventure.key.Key

data class FontData(
    val key: String,
    val id: String,
    val caminho: String,
    /** Deslocamento vertical (ascent) aplicado aos providers bitmap da fonte. */
    val ascent: Int,
    /**
     * Namespace da fonte no pack. `oneblock` gera `assets/oneblock/font/<id>.json`,
     * que é o que `<font:oneblock:gui>` procura.
     */
    val namespace: String = DEFAULT_NAMESPACE,
    /**
     * Altura final dos glyphs, em pixels. `null` mantém a altura declarada em cada
     * provider do JSON; um valor menor que 8 encolhe a fonte proporcionalmente.
     */
    val height: Int? = null,
    /**
     * Ascent do provider principal no JSON de origem. [ascent] é aplicado como
     * deslocamento em cima dele, então esse provider acaba exatamente em [ascent]
     * e os outros mantêm a diferença original.
     */
    val baseAscent: Int = 7,
    /**
     * `true` faz [height] valer como altura literal dos providers em vez de escala
     * relativa aos 8px da vanilla. Usado por fontes de um provider só.
     */
    val absoluteHeight: Boolean = false
) {
    /** Chave da fonte no pack — o que vai em `Component.font(...)`. */
    val fontKey: Key get() = Key.key(namespace, id)

    companion object {
        const val DEFAULT_NAMESPACE = "minecraft"
    }
}
