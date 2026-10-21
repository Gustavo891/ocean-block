package one_block.textura.dialog.node

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEventSource
import net.kyori.adventure.text.format.TextColor
import one_block.textura.api.DialogBinds
import org.bukkit.entity.Player

sealed interface UiSize {
    data class Px(val value: Int) : UiSize
    data object Grow : UiSize
    data object Fit : UiSize
}

enum class UiAlign { Start, Center, End }

enum class UiTextAlign { Left, Center, Right }

enum class ContainerKind { Row, Column, Box, Grid }

data class UiPosition(val x: Int, val y: Int)

sealed interface UiOutcome {
    data object Refresh : UiOutcome
    data object Close : UiOutcome
    data class Page(val id: String) : UiOutcome
    class Open(val screenId: String, val binds: DialogBinds, val values: Map<String, String> = emptyMap()) : UiOutcome
}

class UiAction(val run: (Player) -> UiOutcome)

sealed interface UiNode {
    val w: UiSize
    val h: UiSize
    val position: UiPosition?

    /** Caminho do nó no JSON da tela (o mesmo `path` dos avisos); só serve para editores mapearem caixas. */
    val tag: String?
}

data class ImageNode(
    val id: String,
    override val w: UiSize = UiSize.Fit,
    override val position: UiPosition? = null,
    override val tag: String? = null,
) : UiNode {
    override val h: UiSize get() = UiSize.Fit
}

data class IconNode(
    val id: String,
    val size: Int,
    override val position: UiPosition? = null,
    override val tag: String? = null,
) : UiNode {
    override val w: UiSize get() = UiSize.Px(size)
    override val h: UiSize get() = UiSize.Px(size)
}

data class TextNode(
    val text: String,
    val color: TextColor? = null,
    val bold: Boolean = false,
    val align: UiTextAlign = UiTextAlign.Left,
    override val w: UiSize = UiSize.Fit,
    override val position: UiPosition? = null,
    val shadow: Boolean = true,
    override val tag: String? = null,
) : UiNode {
    override val h: UiSize get() = UiSize.Px(LINE_HEIGHT)

    companion object {
        const val LINE_HEIGHT = 9
    }
}

data class HotspotNode(
    val action: UiAction?,
    val tooltip: HoverEventSource<*>?,
    override val w: UiSize = UiSize.Grow,
    override val h: UiSize = UiSize.Grow,
    override val position: UiPosition? = null,
    override val tag: String? = null,
) : UiNode

data class ContainerNode(
    val kind: ContainerKind,
    val children: List<UiNode>,
    val gap: Int = 0,
    val pad: Int = 0,
    val align: UiAlign = UiAlign.Start,
    val cross: UiAlign = UiAlign.Start,
    val columns: Int = 1,
    val snap: Int = 0,
    val surface: String? = null,
    override val w: UiSize = UiSize.Fit,
    override val h: UiSize = UiSize.Fit,
    override val position: UiPosition? = null,
    override val tag: String? = null,
) : UiNode {

    companion object {
        fun space(px: Int): ContainerNode =
            ContainerNode(ContainerKind.Box, emptyList(), w = UiSize.Px(px), h = UiSize.Px(px))
    }
}

class UiScreen(val width: Int, val root: ContainerNode)

fun UiNode.tagged(tag: String): UiNode = when (this) {
    is ImageNode -> copy(tag = tag)
    is IconNode -> copy(tag = tag)
    is TextNode -> copy(tag = tag)
    is HotspotNode -> copy(tag = tag)
    is ContainerNode -> copy(tag = tag)
}
