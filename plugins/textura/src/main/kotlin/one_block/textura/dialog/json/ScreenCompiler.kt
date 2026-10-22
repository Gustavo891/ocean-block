package one_block.textura.dialog.json

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.event.HoverEventSource
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import one_block.textura.api.TexturaTags
import one_block.textura.dialog.DialogActions
import one_block.textura.dialog.DialogSession
import one_block.textura.dialog.DialogValues
import one_block.textura.dialog.kit.UiArtCatalog
import one_block.textura.dialog.kit.UiKit
import one_block.textura.dialog.layout.UiLayout
import one_block.textura.dialog.node.ContainerKind
import one_block.textura.dialog.node.ContainerNode
import one_block.textura.dialog.node.HotspotNode
import one_block.textura.dialog.node.IconNode
import one_block.textura.dialog.node.ImageNode
import one_block.textura.dialog.node.TextNode
import one_block.textura.dialog.node.UiAction
import one_block.textura.dialog.node.UiAlign
import one_block.textura.dialog.node.UiNode
import one_block.textura.dialog.node.UiOutcome
import one_block.textura.dialog.node.UiPosition
import one_block.textura.dialog.node.UiScreen
import one_block.textura.dialog.node.UiSize
import one_block.textura.dialog.node.UiTextAlign
import one_block.textura.dialog.node.tagged
import one_block.textura.dialog.render.TextMeasurer
import one_block.textura.dialog.theme.UiTheme
import one_block.textura.dialog.theme.UiTextStyle
import one_block.textura.dialog.text.Placeholders
import org.bukkit.Material
import org.bukkit.entity.Player

class ScreenCompiler(private val kit: UiKit, private val warn: (String) -> Unit) {

    private class TooltipLine(val text: String, val component: Component)

    private val measurer = TextMeasurer(kit)
    private val layout = UiLayout(kit)

    fun compile(definition: ScreenDefinition, theme: UiTheme, session: DialogSession): UiScreen {
        val expanded = ListExpander(session::entries, session::listPage).expand(definition.page(session.page))
        val built = Context(theme, session).container(expanded).copy(tag = expanded.path)
        val stretched = built.w == UiSize.Fit && built.kind == ContainerKind.Row && built.children.any { it.w == UiSize.Grow }
        val compiled = if (stretched) built.copy(w = UiSize.Px(STUDIO_WIDTH)) else built
        val wanted = when (val w = compiled.w) {
            is UiSize.Px -> w.value
            UiSize.Grow -> MAX_WIDTH
            UiSize.Fit -> layout.intrinsicWidth(compiled)
        }
        if (wanted > MAX_WIDTH) warn("largura $wanted passa de $MAX_WIDTH px e foi limitada")
        val width = wanted.coerceIn(1, MAX_WIDTH)
        val root = if (compiled.w is UiSize.Px && wanted > MAX_WIDTH) compiled.copy(w = UiSize.Px(width)) else compiled
        return UiScreen(width, ContainerNode(ContainerKind.Box, listOf(root), w = UiSize.Px(width)))
    }

    fun title(definition: ScreenDefinition, session: DialogSession): Component? =
        definition.title?.let { Placeholders(session).rich(it) }

    fun slotIconSize(theme: UiTheme): Int = slotSize(theme) - SLOT_BORDER

    private fun slotSize(theme: UiTheme): Int = kit.image(theme.slots.art)?.height?.takeIf { it > SLOT_BORDER } ?: DEFAULT_SLOT_SIZE

    fun resolveIcon(theme: UiTheme, id: String): String {
        if (!id.startsWith(UiArtCatalog.UI_PREFIX) || kit.has(id)) return id
        val name = id.removePrefix(UiArtCatalog.UI_PREFIX)
        val alias = theme.icons[name] ?: name
        return if (alias.contains(':')) alias else UiArtCatalog.GLYPH_PREFIX + alias
    }

    private inner class Context(private val theme: UiTheme, private val session: DialogSession) {

        private var scope: DefScoped? = null
        private var onSurface = false
        private val placeholders: Placeholders get() = Placeholders(session, scope?.values.orEmpty())

        fun node(def: DefNode): UiNode = build(def).tagged(def.path)

        private fun build(def: DefNode): UiNode = when (def) {
            is DefContainer -> container(def)
            is DefSpace -> if (def.grow) ContainerNode(ContainerKind.Box, emptyList(), w = UiSize.Grow, h = UiSize.Px(0)) else ContainerNode.space(def.px)
            is DefText -> text(def)
            is DefIcon -> iconNode(def)
            is DefImage -> ImageNode(def.id, def.w, def.position)
            is DefSwitch -> switch(def)
            is DefSegmented -> segmented(def)
            is DefButton -> button(def)
            is DefNav -> nav(def)
            is DefDropdown -> dropdown(def)
            is DefSlider -> slider(def)
            is DefSlots -> slots(def)
            is DefChip -> chip(def)
            is DefPair -> pair(def)
            is DefScoped -> scoped(def)
            is DefRepeat -> ContainerNode.space(0)
        }

        private fun scoped(def: DefScoped): UiNode {
            val previous = scope
            scope = def
            return try {
                node(def.node)
            } finally {
                scope = previous
            }
        }

        private fun stateIcon(def: DefIcon): String =
            def.bind?.let { session.value(it) }?.let { def.states[it] } ?: def.id

        private fun iconNode(def: DefIcon): UiNode {
            val id = placeholders.plain(stateIcon(def)).trim()
            if (id.isEmpty() || PLACEHOLDER_MARK in id) {
                return ContainerNode(ContainerKind.Box, emptyList(), w = UiSize.Px(def.size), h = UiSize.Px(def.size), position = def.position)
            }
            return IconNode(icon(id), def.size, def.position)
        }

        private fun clickAction(click: DefClick, bind: String?): UiAction {
            val at = scope
            return UiAction { player ->
                val first = bind?.let { press(it, at, player) } ?: UiOutcome.Refresh
                if (first != UiOutcome.Refresh) first else DialogActions.run(click.steps, player, session, warn, at?.values.orEmpty())
            }
        }

        private fun pressAction(key: String): UiAction {
            val at = scope
            return UiAction { player -> press(key, at, player) }
        }

        private fun press(key: String, at: DefScoped?, player: Player): UiOutcome =
            if (at != null) session.press(key, at.index, player) else session.press(key, player)

        private fun nav(def: DefNav): UiNode {
            val target = (def.click?.steps?.singleOrNull() as? DefStep.Page)?.id
            val active = if (target != null && session.page != null) target == session.page else def.selected.resolve(placeholders::plain)
            val action = when {
                def.click != null -> clickAction(def.click, def.bind)
                def.bind != null -> pressAction(def.bind)
                else -> null
            }
            val style = theme.style(if (active) theme.nav.activeTextStyle else theme.nav.textStyle)
            val label = listOfNotNull(
                ContainerNode.space(theme.nav.pad),
                *labelParts(def.icon, TextNode(placeholders.plain(def.label), style.color, style.bold, shadow = style.shadow)),
            )
            return ContainerNode(
                ContainerKind.Box,
                listOf(
                    ImageNode(if (active) theme.nav.activeArt else theme.nav.art, w = UiSize.Grow),
                    ContainerNode(ContainerKind.Row, label, gap = theme.iconGap, cross = UiAlign.Start),
                    HotspotNode(action, def.tooltip?.let { tooltip(it, null) }),
                ),
                align = UiAlign.Start,
                cross = UiAlign.Center,
                w = def.w,
            )
        }

        private fun slots(def: DefSlots): UiNode {
            val size = slotSize(theme)
            val expand = placeholders::plain
            val items = def.items.mapNotNull { SlotContent.resolve(it, expand) }.associateBy { it.at }
            val rows = (0 until def.rows).map { row ->
                val cells = (0 until def.columns).map { column -> items[row * def.columns + column] }
                val shown = if (def.hideEmpty) cells.filterNotNull() else cells
                ContainerNode(ContainerKind.Row, shown.map { slot(it, size) })
            }
            val height = if (def.hideEmpty) UiSize.Px(size * def.rows) else UiSize.Fit
            return ContainerNode(ContainerKind.Box, listOf(ContainerNode(ContainerKind.Column, rows)), align = def.align, w = UiSize.Grow, h = height)
        }

        private fun slot(item: SlotView?, size: Int): UiNode {
            val art = if (item?.missing == true) theme.slots.missingArt ?: theme.slots.art else theme.slots.art
            val frame = ContainerNode(ContainerKind.Box, listOf(ImageNode(art)), w = UiSize.Px(size), h = UiSize.Px(size))
            if (item == null) return frame
            val style = theme.style(theme.slots.textStyle)
            val children = listOfNotNull(
                frame,
                ContainerNode(
                    ContainerKind.Box,
                    listOf(IconNode(icon(UiArtCatalog.ITEM_PREFIX + item.item), size - SLOT_BORDER)),
                    align = UiAlign.Center,
                    cross = UiAlign.Center,
                    w = UiSize.Px(size),
                    h = UiSize.Px(size),
                ),
                if (item.count > 1) {
                    ContainerNode(
                        ContainerKind.Box,
                        listOf(TextNode(item.count.toString(), style.color, style.bold, shadow = style.shadow)),
                        pad = 1,
                        align = UiAlign.End,
                        cross = UiAlign.End,
                        w = UiSize.Px(size),
                        h = UiSize.Px(size),
                    )
                } else {
                    null
                },
                HotspotNode(null, itemTooltip(item)),
            )
            return ContainerNode(ContainerKind.Box, children, w = UiSize.Px(size), h = UiSize.Px(size))
        }

        private fun itemTooltip(item: SlotView): HoverEventSource<*> {
            if (item.name != null || item.lore.isNotEmpty()) return namedTooltip(item)
            val material = Material.matchMaterial(item.item)?.takeIf { it.isItem } ?: return Component.text(item.item)
            return HoverEvent.showItem(HoverEvent.ShowItem.showItem(material.key(), item.count))
        }

        private fun namedTooltip(item: SlotView): Component {
            val text = placeholders
            val loreColor = theme.style(UiTheme.TOOLTIP_STYLE).color ?: DEFAULT_TOOLTIP_LINE
            val title = item.name?.let { text.rich(it).colorIfAbsent(NamedTextColor.WHITE) } ?: itemName(item.item)
            val lines = listOf(title) + item.lore.map { text.rich(it).colorIfAbsent(loreColor) }
            return Component.join(JoinConfiguration.newlines(), lines.map { it.decoration(TextDecoration.ITALIC, false) })
        }

        private fun itemName(item: String): Component =
            Material.matchMaterial(item)?.let { Component.translatable(it.translationKey()) } ?: Component.text(item)

        private fun dropdown(def: DefDropdown): UiNode {
            if (def.options.isEmpty()) return ContainerNode.space(0)
            val key = def.bind ?: def.path
            val initial = DialogValues.optionId(def.initial)
            val default = def.options.indexOfFirst { DialogValues.optionId(it) == initial }.coerceAtLeast(0)
            val label = def.options[session.choice(key, def.options, default)]
            val style = theme.style(theme.dropdown.textStyle)
            val action = UiAction {
                session.choose(key, def.options, (session.choice(key, def.options, default) + 1) % def.options.size)
                UiOutcome.Refresh
            }
            return ContainerNode(
                ContainerKind.Box,
                listOf(
                    ImageNode(theme.dropdown.art, w = UiSize.Grow),
                    ContainerNode(
                        ContainerKind.Row,
                        listOf(
                            ContainerNode.space(3),
                            TextNode(placeholders.plain(label), style.color, style.bold, w = UiSize.Grow, shadow = style.shadow),
                            ImageNode(theme.dropdown.arrow),
                            ContainerNode.space(3),
                        ),
                        gap = 2,
                        cross = UiAlign.Center,
                        w = UiSize.Grow,
                    ),
                    HotspotNode(action, def.tooltip?.let { tooltip(it, label) }),
                ),
                cross = UiAlign.Center,
                w = def.w,
            )
        }

        private fun slider(def: DefSlider): UiNode {
            val key = def.bind ?: def.path
            val valid = def.max > def.min && def.step > 0
            val current = (session.value(key)?.toIntOrNull() ?: def.value).coerceIn(def.min, maxOf(def.min, def.max))
            val width = (def.w as? UiSize.Px)?.value ?: DEFAULT_SLIDER_WIDTH
            val fraction = if (valid) (current - def.min).toDouble() / (def.max - def.min) else 0.0
            val fillWidth = Math.round(width * fraction).toInt()
            val minimumFill = kit.image(theme.slider.fill)?.slice?.let { it.left + it.right } ?: 1
            val style = theme.style(theme.slider.textStyle)
            val label = "$current${def.unit}"
            val tooltip = def.tooltip?.let { tooltip(it, label) }

            fun move(delta: Int) = UiAction {
                if (valid) {
                    val now = session.value(key)?.toIntOrNull() ?: def.value
                    session.set(key, (now + delta).coerceIn(def.min, def.max).toString())
                }
                UiOutcome.Refresh
            }

            val children = listOfNotNull(
                ImageNode(theme.slider.track, w = UiSize.Grow),
                if (fillWidth >= maxOf(minimumFill, 1)) ImageNode(theme.slider.fill, w = UiSize.Px(fillWidth)) else null,
                TextNode(label, style.color, style.bold, UiTextAlign.Center, UiSize.Grow, shadow = style.shadow),
                ContainerNode(
                    ContainerKind.Row,
                    listOf(HotspotNode(move(-def.step), tooltip), HotspotNode(move(def.step), tooltip)),
                    w = UiSize.Grow,
                    h = UiSize.Grow,
                ),
            )
            return ContainerNode(ContainerKind.Box, children, cross = UiAlign.Center, w = UiSize.Px(width))
        }

        fun container(def: DefContainer): ContainerNode = ContainerNode(
            kind = def.kind,
            children = surfaced(def.surface != null) { def.children.map(::node) },
            gap = def.gap,
            pad = def.pad,
            align = def.align,
            cross = def.cross,
            columns = def.columns,
            snap = def.snap,
            surface = def.surface?.let { name ->
                theme.surfaces[name] ?: run {
                    warn("${def.path}: surface '$name' não existe no tema '${theme.id}'")
                    null
                }
            },
            w = def.w,
            h = def.h,
            position = def.position,
        )

        private fun text(def: DefText): UiNode {
            val tooltip = def.tooltip?.let { tooltip(it, null) }
            val content = textContent(def, if (tooltip == null) def.position else null)
            if (tooltip == null) return content
            return ContainerNode(ContainerKind.Box, listOf(content, HotspotNode(null, tooltip)), w = def.w, position = def.position)
        }

        private fun textContent(def: DefText, position: UiPosition?): UiNode {
            val style = textStyle(def.style?.let(placeholders::plain))
            val color = def.color ?: style.color
            val bold = def.bold ?: style.bold
            val value = placeholders.plain(def.text)
            val iconNode = def.icon?.let(::labelIcon)
            if (iconNode == null) return TextNode(value, color, bold, def.align, def.w, position, style.shadow)

            val label = TextNode(value, color, bold, def.align, if (def.w == UiSize.Grow) UiSize.Grow else UiSize.Fit, shadow = style.shadow)
            return ContainerNode(
                ContainerKind.Row,
                listOf(iconNode, lowered(label)),
                gap = theme.iconGap,
                cross = UiAlign.Start,
                w = def.w,
                position = position,
            )
        }

        private fun switch(def: DefSwitch): UiNode {
            val key = def.bind ?: def.path
            val value = session.value(key)?.let { it == "true" } ?: def.value
            val label = if (value) theme.switch.onLabel else theme.switch.offLabel
            val action = UiAction {
                val now = session.value(key)?.let { it == "true" } ?: def.value
                session.set(key, (!now).toString())
                UiOutcome.Refresh
            }
            return ContainerNode(
                ContainerKind.Box,
                listOf(
                    ImageNode(if (value) theme.switch.on else theme.switch.off),
                    HotspotNode(action, def.tooltip?.let { tooltip(it, label) }),
                ),
            )
        }

        private fun segmented(def: DefSegmented): UiNode {
            if (def.options.isEmpty()) {
                warn("${def.path}: segmented sem opções")
                return ContainerNode.space(0)
            }
            val key = def.bind ?: def.path
            val selected = session.choice(key, def.options, def.selected.coerceIn(0, def.options.lastIndex))
            val normal = theme.style(theme.segmented.textStyle)
            val active = theme.style(theme.segmented.selectedTextStyle)
            val tooltip = def.tooltip?.let { tooltip(it, def.options[selected]) }

            val segments = def.options.mapIndexed { index, option ->
                val chosen = index == selected
                val style = if (chosen) active else normal
                val width = maxOf(measurer.width(option, normal.bold), measurer.width(option, active.bold)) + theme.segmented.pad * 2
                val action = UiAction {
                    session.choose(key, def.options, index)
                    UiOutcome.Refresh
                }
                ContainerNode(
                    ContainerKind.Box,
                    listOf(
                        ImageNode(if (chosen) theme.segmented.selectedArt else theme.segmented.art, w = UiSize.Grow),
                        TextNode(option, style.color, style.bold, shadow = style.shadow),
                        HotspotNode(action, tooltip),
                    ),
                    align = UiAlign.Center,
                    cross = UiAlign.Center,
                    w = UiSize.Px(width),
                )
            }
            val frame = theme.segmented.frame
            return ContainerNode(ContainerKind.Row, segments, pad = if (frame != null) 1 else 0, surface = frame, w = def.w)
        }

        private fun <T> surfaced(inside: Boolean, block: () -> T): T {
            val previous = onSurface
            onSurface = onSurface || inside
            return try {
                block()
            } finally {
                onSurface = previous
            }
        }

        private fun textStyle(name: String?): UiTextStyle =
            if (onSurface) theme.style(name) else theme.styles["${name ?: UiTheme.DEFAULT_STYLE}$BARE_SUFFIX"] ?: theme.style(name)

        private fun chip(def: DefChip): UiNode {
            val style = theme.style(theme.chip.textStyle)
            val content = listOfNotNull(
                ContainerNode.space(theme.chip.pad),
                *labelParts(def.icon, TextNode(placeholders.plain(def.label), style.color, style.bold, shadow = style.shadow)),
                ContainerNode.space(theme.chip.pad),
            )
            return ContainerNode(
                ContainerKind.Box,
                listOfNotNull(
                    ContainerNode(ContainerKind.Row, content, gap = theme.iconGap, cross = UiAlign.Center, h = UiSize.Px(theme.chip.height)),
                    def.tooltip?.let { HotspotNode(null, tooltip(it, null)) },
                ),
                surface = theme.chip.art,
                h = UiSize.Px(theme.chip.height),
            )
        }

        private fun pair(def: DefPair): UiNode {
            val label = textStyle(PAIR_LABEL_STYLE)
            val value = textStyle(def.style)
            return ContainerNode(
                ContainerKind.Row,
                listOf(
                    TextNode(placeholders.plain(def.label), label.color, label.bold, w = UiSize.Grow, shadow = label.shadow),
                    TextNode(placeholders.plain(def.value), value.color, value.bold, UiTextAlign.Right, shadow = value.shadow),
                ),
                gap = theme.iconGap,
                w = def.w,
            )
        }

        private fun button(def: DefButton): UiNode {
            val key = def.bind ?: def.path
            val look = theme.button.look(def.look)
            val disabled = def.disabled.resolve(placeholders::plain)
            val selected = def.selected.resolve(placeholders::plain)
            val art = when {
                disabled -> look.disabledArt
                selected -> look.selectedArt
                else -> look.art
            }
            val style = theme.style(
                when {
                    disabled -> look.disabledTextStyle
                    selected -> look.selectedTextStyle
                    else -> look.textStyle
                },
            )
            val label = listOfNotNull(
                ContainerNode.space(1),
                *labelParts(def.icon, TextNode(placeholders.plain(def.label), style.color, style.bold, shadow = style.shadow)),
                ContainerNode.space(1),
            )
            val action = when {
                disabled -> null
                def.click != null -> clickAction(def.click, def.bind)
                else -> pressAction(key)
            }
            return ContainerNode(
                ContainerKind.Box,
                listOf(
                    ImageNode(art, w = UiSize.Grow),
                    ContainerNode(ContainerKind.Row, label, gap = theme.iconGap, cross = UiAlign.Start),
                    HotspotNode(action, def.tooltip?.let { tooltip(it, null) }),
                ),
                align = UiAlign.Center,
                cross = UiAlign.Center,
                w = def.w,
            )
        }

        private fun icon(id: String): String = resolveIcon(theme, id)

        private fun labelIcon(raw: String): IconNode? {
            val id = placeholders.plain(raw).trim()
            if (id.isEmpty() || PLACEHOLDER_MARK in id) return null
            return icon(id).takeIf(kit::has)?.let { IconNode(it, theme.iconSize) }
        }

        private fun labelParts(iconId: String?, text: TextNode): Array<UiNode?> {
            val iconNode = iconId?.let(::labelIcon) ?: return arrayOf(null, text)
            return arrayOf(iconNode, lowered(text))
        }

        private fun lowered(text: TextNode): UiNode =
            ContainerNode(ContainerKind.Column, listOf(ContainerNode.space(ICON_RISE), text), w = text.w)

        private fun tooltip(text: String, value: String?): Component {
            val extra = if (value == null) emptyMap() else mapOf(Placeholders.VALUE_KEY to value)
            val placeholders = Placeholders(session, scope?.values.orEmpty() + extra)
            val body = theme.style(UiTheme.TOOLTIP_STYLE).color
            if (text.contains('<')) return placeholders.rich(text).colorIfAbsent(body)
            val lines = tooltipLines(text, placeholders)
            if (lines.size == 1) return lines.single().component.colorIfAbsent(body)

            val texts = lines.map { it.text }
            val action = texts.lastIndex.takeIf { it >= 2 && texts[it].isNotBlank() && texts[it - 1].isBlank() }
            val parts = lines.mapIndexed { index, line ->
                val color = when {
                    index == 0 -> styleColor(TOOLTIP_TITLE, DEFAULT_TOOLTIP_TITLE)
                    index == action -> actionColor(line.text)
                    HIGHLIGHT_MARKS.any(line.text::startsWith) -> styleColor(TOOLTIP_HIGHLIGHT, DEFAULT_TOOLTIP_HIGHLIGHT)
                    line.text.isNotBlank() && texts[index - 1].isBlank() -> styleColor(TOOLTIP_HEADER, DEFAULT_TOOLTIP_HEADER)
                    else -> styleColor(TOOLTIP_LINE, DEFAULT_TOOLTIP_LINE)
                }
                line.component.colorIfAbsent(color)
            }
            return Component.join(JoinConfiguration.newlines(), parts)
        }

        private fun tooltipLines(text: String, placeholders: Placeholders): List<TooltipLine> =
            text.split('\n').flatMap { line ->
                val resolved = placeholders.escaped(line)
                if ('\n' in resolved) {
                    resolved.split('\n').map { TooltipLine(it, TexturaTags.miniMessage.deserialize(it)) }
                } else {
                    listOf(TooltipLine(resolved, placeholders.rich(line)))
                }
            }

        private fun actionColor(line: String): TextColor = when {
            line.startsWith(SUCCESS_MARK) -> styleColor(TOOLTIP_SUCCESS, DEFAULT_TOOLTIP_SUCCESS)
            ERROR_MARKS.any(line::startsWith) -> styleColor(TOOLTIP_ERROR, DEFAULT_TOOLTIP_ERROR)
            else -> styleColor(TOOLTIP_ACTION, DEFAULT_TOOLTIP_ACTION)
        }

        private fun styleColor(name: String, default: TextColor): TextColor = theme.styles[name]?.color ?: default
    }

    companion object {
        const val MAX_WIDTH = 399
        const val DEFAULT_SLIDER_WIDTH = 80
        const val DEFAULT_SLOT_SIZE = 18
        const val STUDIO_WIDTH = 388
        private const val SLOT_BORDER = 2
        private const val PLACEHOLDER_MARK = '{'
        private const val ICON_RISE = 1
        private const val BARE_SUFFIX = "-bare"
        private const val PAIR_LABEL_STYLE = "muted"
        private const val TOOLTIP_TITLE = "tooltip-title"
        private const val TOOLTIP_LINE = "tooltip-line"
        private const val TOOLTIP_HIGHLIGHT = "tooltip-highlight"
        private const val TOOLTIP_ACTION = "tooltip-action"
        private const val TOOLTIP_HEADER = "tooltip-header"
        private const val TOOLTIP_SUCCESS = "tooltip-success"
        private const val TOOLTIP_ERROR = "tooltip-error"
        private val HIGHLIGHT_MARKS = listOf(Char(0x23F5).toString(), Char(0x2219).toString())
        private val SUCCESS_MARK = Char(0x2714).toString()
        private val ERROR_MARKS = listOf(Char(0x2300).toString(), Char(0x2716).toString())
        private val DEFAULT_TOOLTIP_HEADER: TextColor = TextColor.color(0xFFFFFF)
        private val DEFAULT_TOOLTIP_SUCCESS: TextColor = TextColor.color(0x6FE06F)
        private val DEFAULT_TOOLTIP_ERROR: TextColor = TextColor.color(0xFF6B6B)
        private val DEFAULT_TOOLTIP_TITLE: TextColor = TextColor.color(0xFFD56A)
        private val DEFAULT_TOOLTIP_LINE: TextColor = TextColor.color(0xA8A8B3)
        private val DEFAULT_TOOLTIP_HIGHLIGHT: TextColor = TextColor.color(0xE6E6E6)
        private val DEFAULT_TOOLTIP_ACTION: TextColor = TextColor.color(0xFFE066)
    }
}
