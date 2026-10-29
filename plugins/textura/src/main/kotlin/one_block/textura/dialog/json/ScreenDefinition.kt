package one_block.textura.dialog.json

import net.kyori.adventure.text.format.TextColor
import one_block.textura.dialog.node.ContainerKind
import one_block.textura.dialog.node.UiAlign
import one_block.textura.dialog.node.UiPosition
import one_block.textura.dialog.node.UiSize
import one_block.textura.dialog.node.UiTextAlign

sealed interface DefNode {
    val path: String
}

data class DefContainer(
    val kind: ContainerKind,
    val children: List<DefNode>,
    val w: UiSize,
    val h: UiSize,
    val gap: Int,
    val pad: Int,
    val align: UiAlign,
    val cross: UiAlign,
    val snap: Int,
    val columns: Int,
    val surface: String?,
    val position: UiPosition?,
    override val path: String,
) : DefNode

data class DefSpace(val px: Int, override val path: String, val grow: Boolean = false) : DefNode

data class DefText(
    val text: String,
    val style: String?,
    val align: UiTextAlign,
    val w: UiSize,
    val icon: String?,
    val color: TextColor?,
    val bold: Boolean?,
    val tooltip: String?,
    val position: UiPosition?,
    override val path: String,
) : DefNode

data class DefIcon(
    val id: String,
    val size: Int,
    val position: UiPosition?,
    override val path: String,
    val bind: String? = null,
    val states: Map<String, String> = emptyMap(),
    val frame: Boolean = false,
) : DefNode

data class DefImage(val id: String, val w: UiSize, val position: UiPosition?, override val path: String) : DefNode

data class DefSwitch(
    val value: Boolean,
    val bind: String?,
    val tooltip: String?,
    override val path: String,
    val state: DefFlag? = null,
) : DefNode

data class DefSegmented(
    val options: List<String>,
    val selected: Int,
    val bind: String?,
    val w: UiSize,
    val tooltip: String?,
    override val path: String,
) : DefNode

data class DefButton(
    val label: String,
    val bind: String?,
    val w: UiSize,
    val icon: String?,
    val tooltip: String?,
    override val path: String,
    val click: DefClick? = null,
    val look: String? = null,
    val selected: DefFlag = DefFlag.OFF,
    val disabled: DefFlag = DefFlag.OFF,
) : DefNode

data class DefChip(val label: String, val icon: String?, val tooltip: String?, override val path: String) : DefNode

data class DefPair(val label: String, val value: String, val style: String?, val w: UiSize, override val path: String) : DefNode

data class DefNav(
    val label: String,
    val icon: String?,
    val w: UiSize,
    val click: DefClick?,
    override val path: String,
    val selected: DefFlag = DefFlag.OFF,
    val tooltip: String? = null,
    val bind: String? = null,
) : DefNode

data class DefSlotItem(
    val at: Int,
    val item: String,
    val count: String,
    val name: String? = null,
    val lore: List<String> = emptyList(),
    val missing: DefFlag = DefFlag.OFF,
)

data class DefSlots(
    val columns: Int,
    val rows: Int,
    val items: List<DefSlotItem>,
    val align: UiAlign,
    val hideEmpty: Boolean = false,
    override val path: String,
    val bind: String? = null,
) : DefNode

data class DefDropdown(
    val initial: String,
    val options: List<String>,
    val bind: String?,
    val w: UiSize,
    val tooltip: String?,
    override val path: String,
) : DefNode

data class DefSlider(
    val value: Int,
    val min: Int,
    val max: Int,
    val step: Int,
    val unit: String,
    val bind: String?,
    val w: UiSize,
    val tooltip: String?,
    override val path: String,
) : DefNode

data class DefProgress(
    val value: String,
    val max: String,
    val track: String,
    val fill: String,
    val w: UiSize,
    override val path: String,
) : DefNode

data class DefPixels(
    val bind: String,
    val w: Int,
    val h: Int,
    val scale: Int,
    val position: UiPosition?,
    override val path: String,
    val detail: Int = 1,
) : DefNode

data class DefRepeat(val list: String, val perPage: Int?, val node: DefNode, override val path: String) : DefNode

data class DefScoped(
    val values: Map<String, String>,
    val list: String,
    val index: Int,
    val node: DefNode,
    override val path: String,
) : DefNode

sealed interface DefFlag {
    fun resolve(expand: (String) -> String): Boolean

    data class Fixed(val value: Boolean) : DefFlag {
        override fun resolve(expand: (String) -> String): Boolean = value
    }

    data class Bound(val raw: String) : DefFlag {
        override fun resolve(expand: (String) -> String): Boolean = expand(raw).trim().equals(TRUE, ignoreCase = true)
    }

    companion object {
        private const val TRUE = "true"
        val OFF: DefFlag = Fixed(false)
    }
}

sealed interface DefStep {
    data class Page(val id: String) : DefStep
    data object Close : DefStep
    data object Refresh : DefStep
    data class Open(val id: String, val args: Map<String, String>) : DefStep
    data class Set(val pairs: Map<String, String>) : DefStep
    data class Message(val text: String) : DefStep
    data class Command(val console: Boolean, val line: String) : DefStep
    data class Sound(val id: String, val volume: Float, val pitch: Float) : DefStep
}

data class DefClick(val steps: List<DefStep>)

data class DefInput(val key: String, val label: String, val maxLength: Int, val width: Int)

data class DefSubmit(val label: String, val bind: String)

data class ScreenDefinition(
    val id: String,
    val theme: String,
    val title: String?,
    val close: String?,
    val root: DefContainer,
    val keys: Set<String>,
    val controlBinds: Set<String>,
    val pages: Map<String, DefContainer> = emptyMap(),
    val firstPage: String? = null,
    val escape: Boolean = true,
    val pause: Boolean = false,
    val defaults: Map<String, String> = emptyMap(),
    val choices: Map<String, List<String>> = emptyMap(),
    val input: DefInput? = null,
    val submit: DefSubmit? = null,
    val footer: Boolean = true,
) {
    fun page(id: String?): DefContainer = id?.let { pages[it] } ?: root

    fun containers(): List<DefContainer> = (listOf(root) + pages.values).distinct()
}
