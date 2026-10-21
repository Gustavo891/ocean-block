package one_block.textura.dialog.layout

import net.kyori.adventure.text.event.HoverEventSource
import net.kyori.adventure.text.format.TextColor
import one_block.textura.dialog.node.UiAction

sealed interface ImageVariant {
    data object Plain : ImageVariant
    data class Icon(val size: Int) : ImageVariant
    data object Surface : ImageVariant
}

data class PlacedImage(val id: String, val x: Int, val y: Int, val width: Int, val height: Int, val variant: ImageVariant)

data class PlacedText(val text: String, val color: TextColor?, val bold: Boolean, val x: Int, val y: Int, val maxWidth: Int, val shadow: Boolean = true)

data class PlacedHotspot(val x: Int, val y: Int, val w: Int, val h: Int, val action: UiAction?, val tooltip: HoverEventSource<*>?)

/** Área ocupada por um nó marcado com `tag` (caminho no JSON); usada por editores, não pelo jogo. */
data class PlacedBox(val tag: String, val x: Int, val y: Int, val w: Int, val h: Int)

data class LaidOut(
    val width: Int,
    val height: Int,
    val images: List<PlacedImage>,
    val texts: List<PlacedText>,
    val hotspots: List<PlacedHotspot>,
    val boxes: List<PlacedBox> = emptyList(),
)
