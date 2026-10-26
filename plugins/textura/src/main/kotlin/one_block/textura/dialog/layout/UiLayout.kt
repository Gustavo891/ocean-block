package one_block.textura.dialog.layout

import one_block.textura.dialog.kit.IconScale
import one_block.textura.dialog.kit.IconSize
import one_block.textura.dialog.kit.UiKit
import one_block.textura.dialog.node.ContainerKind
import one_block.textura.dialog.node.ContainerNode
import one_block.textura.dialog.node.HotspotNode
import one_block.textura.dialog.node.IconNode
import one_block.textura.dialog.node.ImageNode
import one_block.textura.dialog.node.PixelsNode
import one_block.textura.dialog.node.TextNode
import one_block.textura.dialog.node.UiAlign
import one_block.textura.dialog.node.UiNode
import one_block.textura.dialog.node.UiScreen
import one_block.textura.dialog.node.UiSize
import one_block.textura.dialog.node.UiTextAlign
import one_block.textura.dialog.render.TextMeasurer
import java.util.IdentityHashMap

class UiLayout(private val kit: UiKit) {

    private val measurer = TextMeasurer(kit)
    private val widths = IdentityHashMap<UiNode, Int>()
    private val heights = IdentityHashMap<UiNode, HashMap<HeightKey, Int>>()

    private data class HeightKey(val y: Int, val width: Int, val parentHeight: Int?)

    private class Sink {
        val images = mutableListOf<PlacedImage>()
        val texts = mutableListOf<PlacedText>()
        val hotspots = mutableListOf<PlacedHotspot>()
        val boxes = mutableListOf<PlacedBox>()
        val pixels = mutableListOf<PlacedPixels>()
    }

    private data class Area(val x: Int, val y: Int, val width: Int, val height: Int?)

    fun layout(screen: UiScreen): LaidOut {
        val sink = Sink()
        val height = place(screen.root, 0, 0, screen.width, null, sink)
        return LaidOut(screen.width, height, sink.images.toList(), sink.texts.toList(), sink.hotspots.toList(), sink.boxes.toList(), sink.pixels.toList())
    }

    fun intrinsicWidth(node: UiNode): Int {
        (node.w as? UiSize.Px)?.let { return it.value }
        widths[node]?.let { return it }
        return computeWidth(node).also { widths[node] = it }
    }

    private fun computeWidth(node: UiNode): Int {
        return when (node) {
            is ImageNode -> kit.image(node.id)?.let { image -> image.slice?.let { it.left + it.right } ?: image.width } ?: 0
            is IconNode -> node.size
            is TextNode -> measurer.width(node.text, node.bold, node.font)
            is HotspotNode -> 0
            is PixelsNode -> (node.w as? UiSize.Px)?.value ?: (node.image.width * node.scale)
            is ContainerNode -> {
                val flow = node.children.filter { it.position == null }.map(::intrinsicWidth)
                node.pad * 2 + when (node.kind) {
                    ContainerKind.Row -> flow.sum() + node.gap * (flow.size - 1).coerceAtLeast(0)
                    ContainerKind.Column, ContainerKind.Box -> flow.maxOrNull() ?: 0
                    ContainerKind.Grid -> (flow.maxOrNull() ?: 0) * node.columns + node.gap * (node.columns - 1)
                }
            }
        }
    }

    private fun widthIn(child: UiNode, available: Int): Int = when (val w = child.w) {
        is UiSize.Px -> w.value
        UiSize.Grow -> available
        UiSize.Fit -> intrinsicWidth(child).coerceAtMost(available)
    }

    private fun measureHeight(node: UiNode, y: Int, width: Int, parentHeight: Int?): Int {
        val key = HeightKey(y, width, parentHeight)
        val cache = heights.getOrPut(node) { HashMap() }
        return cache.getOrPut(key) { place(node, 0, y, width, parentHeight, Sink()) }
    }

    private fun place(node: UiNode, x: Int, y: Int, width: Int, parentHeight: Int?, sink: Sink): Int {
        val height = when (node) {
            is ImageNode -> placeImage(node, x, y, width, sink)
            is IconNode -> placeIcon(node, x, y, sink)
            is TextNode -> placeText(node, x, y, width, sink)
            is HotspotNode -> placeHotspot(node, x, y, width, parentHeight, sink)
            is ContainerNode -> placeContainer(node, x, y, width, parentHeight, sink)
            is PixelsNode -> placePixels(node, x, y, width, sink)
        }
        node.tag?.let { sink.boxes += PlacedBox(it, x, y, if (node is IconNode) node.size else width, height) }
        return height
    }

    private fun placeImage(node: ImageNode, x: Int, y: Int, width: Int, sink: Sink): Int {
        val height = kit.image(node.id)?.height ?: 0
        sink.images += PlacedImage(node.id, x, y, width, height, ImageVariant.Plain)
        return height
    }

    private fun placeIcon(node: IconNode, x: Int, y: Int, sink: Sink): Int {
        val drawn = kit.icon(IconSize(node.id, node.size))?.width
            ?: kit.image(node.id)?.let { IconScale.width(it.width, it.height, node.size) }
            ?: node.size
        val offset = ((node.size - drawn) / 2).coerceAtLeast(0)
        sink.images += PlacedImage(node.id, x + offset, y, drawn, node.size, ImageVariant.Icon(node.size))
        return node.size
    }

    private fun placeText(node: TextNode, x: Int, y: Int, width: Int, sink: Sink): Int {
        val textWidth = measurer.width(node.text, node.bold, node.font)
        val offset = when (node.align) {
            UiTextAlign.Left -> 0
            UiTextAlign.Center -> ((width - textWidth + 1) / 2).coerceAtLeast(0)
            UiTextAlign.Right -> (width - textWidth).coerceAtLeast(0)
        }
        sink.texts += PlacedText(node.text, node.color, node.bold, x + offset, y, width - offset, node.shadow, node.font)
        return TextNode.LINE_HEIGHT
    }

    private fun placePixels(node: PixelsNode, x: Int, y: Int, width: Int, sink: Sink): Int {
        val height = (node.h as? UiSize.Px)?.value ?: (node.image.height * node.scale)
        sink.pixels += PlacedPixels(node.image, x, y, width, height, node.scale)
        return height
    }

    private fun placeHotspot(node: HotspotNode, x: Int, y: Int, width: Int, parentHeight: Int?, sink: Sink): Int {
        val height = when (val size = node.h) {
            is UiSize.Px -> size.value
            else -> parentHeight ?: 0
        }
        sink.hotspots += PlacedHotspot(x, y, width, height, node.action, node.tooltip)
        return height
    }

    private fun placeContainer(node: ContainerNode, x: Int, y: Int, width: Int, parentHeight: Int?, sink: Sink): Int {
        val fixedHeight = when (val h = node.h) {
            is UiSize.Px -> h.value
            UiSize.Grow -> parentHeight
            UiSize.Fit -> null
        }
        val surfaceAt = sink.images.size
        val area = Area(
            x + node.pad,
            y + node.pad,
            (width - node.pad * 2).coerceAtLeast(0),
            fixedHeight?.let { (it - node.pad * 2).coerceAtLeast(0) },
        )
        val flow = node.children.filter { it.position == null }
        val content = when (node.kind) {
            ContainerKind.Row -> placeRow(node, flow, area, sink)
            ContainerKind.Column -> placeColumn(node, flow, area, sink)
            ContainerKind.Box -> placeBox(node, flow, area, sink)
            ContainerKind.Grid -> placeGrid(node, flow, area, sink)
        }
        val positionedBottom = node.children.mapNotNull { child ->
            val position = child.position ?: return@mapNotNull null
            val available = (width - position.x).coerceAtLeast(0)
            position.y + place(child, x + position.x, y + position.y, widthIn(child, available), null, sink)
        }.maxOrNull() ?: 0

        val height = fixedHeight ?: maxOf(content + node.pad * 2, positionedBottom)
        node.surface?.let { sink.images.add(surfaceAt, PlacedImage(it, x, y, width, height, ImageVariant.Surface)) }
        return height
    }

    private fun placeRow(node: ContainerNode, flow: List<UiNode>, area: Area, sink: Sink): Int {
        val gaps = node.gap * (flow.size - 1).coerceAtLeast(0)
        val grows = flow.count { it.w == UiSize.Grow }
        var remaining = (area.width - gaps).coerceAtLeast(0)
        val assigned = flow.map { child ->
            if (child.w == UiSize.Grow) 0 else widthIn(child, remaining).also { remaining = (remaining - it).coerceAtLeast(0) }
        }
        val share = if (grows == 0) 0 else remaining / grows
        val extra = if (grows == 0) 0 else remaining % grows
        var growIndex = 0
        val widths = flow.mapIndexed { i, child ->
            if (child.w == UiSize.Grow) share + (if (growIndex++ < extra) 1 else 0) else assigned[i]
        }
        val heights = flow.mapIndexed { i, child -> measureHeight(child, area.y, widths[i], area.height) }
        val rowHeight = area.height ?: (heights.maxOrNull() ?: 0)
        var cx = area.x + if (grows == 0) align(area.width - widths.sum() - gaps, node.align) else 0

        flow.forEachIndexed { i, child ->
            val childHeight = if (child.h == UiSize.Grow) rowHeight else heights[i]
            place(child, cx, area.y + align(rowHeight - childHeight, node.cross), widths[i], rowHeight, sink)
            cx += widths[i] + node.gap
        }
        return rowHeight
    }

    private fun placeColumn(node: ContainerNode, flow: List<UiNode>, area: Area, sink: Sink): Int {
        var cy = area.y
        flow.forEach { child ->
            cy = snapped(cy, node.snap)
            val w = widthIn(child, area.width)
            val h = place(child, area.x + align(area.width - w, node.cross), cy, w, area.height, sink)
            cy += h + node.gap
        }
        return (cy - area.y - if (flow.isEmpty()) 0 else node.gap).coerceAtLeast(0)
    }

    private fun placeBox(node: ContainerNode, flow: List<UiNode>, area: Area, sink: Sink): Int {
        val widths = flow.map { widthIn(it, area.width) }
        val heights = flow.mapIndexed { i, child -> measureHeight(child, area.y, widths[i], area.height) }
        val boxHeight = area.height ?: (heights.maxOrNull() ?: 0)

        flow.forEachIndexed { i, child ->
            val childHeight = if (child.h == UiSize.Grow) boxHeight else heights[i]
            place(
                child,
                area.x + align(area.width - widths[i], node.align),
                area.y + align(boxHeight - childHeight, node.cross),
                widths[i],
                boxHeight,
                sink,
            )
        }
        return boxHeight
    }

    private fun placeGrid(node: ContainerNode, flow: List<UiNode>, area: Area, sink: Sink): Int {
        val room = (area.width - node.gap * (node.columns - 1)).coerceAtLeast(0)
        val cells = List(node.columns) { room / node.columns + if (it < room % node.columns) 1 else 0 }
        val starts = cells.runningFold(0) { acc, cell -> acc + cell + node.gap }
        var cy = area.y

        flow.chunked(node.columns).forEach { line ->
            cy = snapped(cy, node.snap)
            val heights = line.mapIndexed { i, child -> measureHeight(child, cy, widthIn(child, cells[i]), null) }
            val lineHeight = heights.maxOrNull() ?: 0
            line.forEachIndexed { i, child -> place(child, area.x + starts[i], cy, widthIn(child, cells[i]), lineHeight, sink) }
            cy += lineHeight + node.gap
        }
        return (cy - area.y - if (flow.isEmpty()) 0 else node.gap).coerceAtLeast(0)
    }

    private fun snapped(y: Int, snap: Int): Int = if (snap > 0) (y + snap - 1) / snap * snap else y

    private fun align(free: Int, align: UiAlign): Int = when (align) {
        UiAlign.Start -> 0
        UiAlign.Center -> (free + 1) / 2
        UiAlign.End -> free
    }.coerceAtLeast(0)
}
