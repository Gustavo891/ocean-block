package one_block.textura.dialog.render

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor
import one_block.textura.dialog.kit.UiPixels
import net.kyori.adventure.text.format.TextDecoration
import one_block.textura.dialog.kit.IconSize
import one_block.textura.dialog.kit.SurfaceSize
import one_block.textura.dialog.kit.UiImage
import one_block.textura.dialog.kit.UiKit
import one_block.textura.dialog.kit.UiSlice
import one_block.textura.dialog.layout.ImageVariant
import one_block.textura.dialog.layout.LaidOut
import one_block.textura.dialog.layout.PlacedHotspot
import one_block.textura.dialog.layout.PlacedImage
import one_block.textura.dialog.layout.PlacedText
import one_block.textura.dialog.node.UiAction

class UiRenderer(
    private val kit: UiKit,
    private val click: (UiAction) -> ClickEvent<*>,
    private val warn: (String) -> Unit,
) {

    private val spaces = Spaces(kit)
    private val measurer = TextMeasurer(kit)
    private val clicks = HashMap<PlacedHotspot, ClickEvent<*>>()

    private data class Span(val start: Int, val end: Int, val hotspot: PlacedHotspot)

    private inner class Line {
        val builder = Component.text()
        var cursor = 0

        fun moveTo(target: Int) {
            val text = spaces.of(target - cursor)
            if (text.isNotEmpty()) builder.append(Component.text(text).font(kit.spaceFont))
            cursor = target
        }

        fun append(part: Component, advance: Int) {
            builder.append(part)
            cursor += advance
        }
    }

    fun render(laid: LaidOut): List<Component> {
        val body = bodyWidth(laid.width)
        val margin = (body - laid.width) / 2
        val count = maxOf(1, (laid.height + LINE - 1) / LINE)

        val imagesByLine = laid.images.groupBy { it.y / LINE }
        val textsByLine = laid.texts.groupBy { it.y / LINE }
        val pixelsByLine = pixelRows(laid).groupBy { it.y / LINE }

        return (0 until count).map { n ->
            val line = Line()
            val spans = spans(laid, n)
            spans.forEach { hotspot(line, margin, it) }
            markers(line, margin, spans)
            line.moveTo(0)
            imagesByLine[n].orEmpty().forEach { image(line, margin, it, laid.width) }
            pixelsByLine[n].orEmpty().forEach { pixelRow(line, margin, it) }
            textsByLine[n].orEmpty().sortedBy { it.x }.forEach { text(line, margin, it, laid.width) }
            line.moveTo(body)
            line.builder.build()
        }
    }

    private class PixelRow(val y: Int, val x: Int, val scale: Int, val runs: List<PixelRun>)

    private fun pixelRows(laid: LaidOut): List<PixelRow> = laid.pixels.flatMap { placed ->
        val scale = placed.scale
        val columns = minOf(placed.image.width, placed.width / scale, (laid.width - placed.x) / scale)
        val rows = minOf(placed.image.height, placed.height / scale)
        if (columns <= 0 || rows <= 0) return@flatMap emptyList()
        if (kit.pixel(scale, 1) == null) {
            warn("Pixels em escala $scale não estão no pack — rode /textura gerar")
            return@flatMap emptyList()
        }
        (0 until rows).map { row ->
            val argb = placed.image.getRGB(0, row, columns, 1, null, 0, columns)
            PixelRow(placed.y + row * scale, placed.x, scale, PixelRuns.of(argb))
        }
    }

    private fun pixelRow(line: Line, margin: Int, row: PixelRow) {
        val font = kit.imageFont(row.y % LINE)
        row.runs.flatMap { PixelRuns.pieces(it, UiPixels.MAX_WIDTH) }.forEach { piece ->
            val glyph = kit.pixel(row.scale, piece.width) ?: return@forEach
            line.moveTo(margin + row.x + piece.x * row.scale)
            line.append(Component.text(glyph.char).font(font).color(TextColor.color(piece.rgb)).shadowColor(NO_SHADOW), glyph.advance)
        }
    }

    private fun spans(laid: LaidOut, n: Int): List<Span> {
        val top = n * LINE
        val bottom = top + LINE
        val ordered = laid.hotspots
            .filter { it.y < bottom && it.y + it.h > top && (it.action != null || it.tooltip != null) }
            .sortedWith(
                compareByDescending<PlacedHotspot> { minOf(it.y + it.h, bottom) - maxOf(it.y, top) }
                    .thenBy { it.y }
                    .thenBy { it.x },
            )
        val claimed = mutableListOf<Span>()

        ordered.forEach { hotspot ->
            if (hotspot.x + hotspot.w > laid.width) warn("Área clicável em x=${hotspot.x} passa da largura ${laid.width} e foi cortada")
            if (claimed.any { overlaps(it.hotspot, hotspot) }) warn("Área clicável em x=${hotspot.x} sobrepõe outra na mesma linha")

            claimed += free(maxOf(hotspot.x, 0), minOf(hotspot.x + hotspot.w, laid.width), claimed)
                .map { (start, end) -> Span(start, end, hotspot) }
        }
        return claimed.sortedBy { it.start }
    }

    private fun overlaps(a: PlacedHotspot, b: PlacedHotspot): Boolean = a.x < b.x + b.w && b.x < a.x + a.w

    private fun free(start: Int, end: Int, claimed: List<Span>): List<Pair<Int, Int>> =
        claimed.fold(listOf(start to end)) { parts, span ->
            parts.flatMap { (s, e) ->
                if (span.end <= s || span.start >= e) {
                    listOf(s to e)
                } else {
                    listOf(s to span.start, span.end to e).filter { it.first < it.second }
                }
            }
        }.filter { it.first < it.second }

    private fun hotspot(line: Line, margin: Int, span: Span) {
        line.moveTo(margin + span.start)
        var part: TextComponent = Component.text(spaces.of(span.end - span.start)).font(kit.spaceFont)
        span.hotspot.action?.let { action -> part = part.clickEvent(clicks.getOrPut(span.hotspot) { click(action) }) }
        span.hotspot.tooltip?.let { part = part.hoverEvent(it) }
        line.append(part, span.end - span.start)
    }

    private fun markers(line: Line, margin: Int, spans: List<Span>) {
        val hit = kit.hit ?: return
        if (spans.isEmpty()) return

        val font = kit.imageFont(0)
        line.moveTo(margin + spans.minOf { it.start })
        line.append(Component.text(hit.char).font(font), hit.advance)
        line.moveTo(margin + spans.maxOf { it.end } - 1)
        line.append(Component.text(hit.char).font(font), hit.advance)
    }

    private fun image(line: Line, margin: Int, placed: PlacedImage, width: Int) {
        val image = resolve(placed) ?: return
        val drawWidth = if (image.slice != null) placed.width else image.width

        if (placed.x < 0 || placed.x + drawWidth > width) {
            warn("Imagem '${placed.id}' em x=${placed.x} passa da largura $width e foi descartada")
            return
        }

        val font = kit.imageFont(placed.y % LINE)
        line.moveTo(margin + placed.x)

        val slice = image.slice
        if (slice == null) {
            line.append(Component.text(image.char).font(font).shadowColor(NO_SHADOW), image.advance)
            return
        }
        sliced(line, margin, placed, slice, font)
    }

    private fun resolve(placed: PlacedImage): UiImage? = when (val variant = placed.variant) {
        ImageVariant.Plain -> kit.image(placed.id) ?: run {
            warn("Imagem '${placed.id}' não existe")
            null
        }
        is ImageVariant.Icon -> kit.icon(IconSize(placed.id, variant.size)) ?: run {
            warn("Ícone '${placed.id}' em ${variant.size} px não está no pack (inexistente ou não pré-gerado — rode /textura gerar)")
            null
        }
        ImageVariant.Surface -> kit.surface(SurfaceSize(placed.id, placed.width, placed.height)) ?: run {
            warn("Surface '${placed.id}' ${placed.width}x${placed.height} não pré-gerada — rode /textura gerar")
            null
        }
    }

    private fun sliced(line: Line, margin: Int, placed: PlacedImage, slice: UiSlice, font: Key) {
        if (placed.width < slice.left + slice.right) {
            warn("Imagem '${placed.id}' mais estreita que as tampas")
            return
        }

        slice.leftCap?.let { line.append(Component.text(it.char).font(font).shadowColor(NO_SHADOW), it.advance) }

        var x = placed.x + slice.left
        var left = placed.width - slice.left - slice.right
        val sizes = slice.middles.keys.sortedDescending()

        while (left > 0) {
            val size = sizes.firstOrNull { it <= left } ?: break
            val piece = slice.middles.getValue(size)
            line.moveTo(margin + x)
            line.append(Component.text(piece.char).font(font).shadowColor(NO_SHADOW), piece.advance)
            x += size
            left -= size
        }

        slice.rightCap?.let { cap ->
            line.moveTo(margin + placed.x + placed.width - slice.right)
            line.append(Component.text(cap.char).font(font).shadowColor(NO_SHADOW), cap.advance)
        }
    }

    private fun text(line: Line, margin: Int, placed: PlacedText, width: Int) {
        val room = minOf(placed.maxWidth, width - placed.x)
        if (room <= 0) {
            warn("Texto '${placed.text}' começa fora da largura e foi descartado")
            return
        }

        val content = measurer.fit(placed.text, placed.bold, room)
        if (content.isEmpty()) {
            if (placed.text.isNotEmpty()) warn("Texto '${placed.text}' não cabe em $room px e foi descartado")
            return
        }
        if (content != placed.text) warn("Texto '${placed.text}' cortado para caber em $room px")

        var part = Component.text(content).font(kit.textFont(placed.y % LINE))
        placed.color?.let { part = part.color(it) }
        if (placed.bold) part = part.decoration(TextDecoration.BOLD, true)
        if (!placed.shadow) part = part.shadowColor(NO_SHADOW)

        line.moveTo(margin + placed.x)
        line.append(part, measurer.width(content, placed.bold))
    }

    companion object {
        const val LINE = 9
        private val NO_SHADOW: ShadowColor = ShadowColor.shadowColor(0)
        private const val WIDGET_PADDING = 8
        private const val SMALL_BODY = 256
        private const val LARGE_BODY = 400

        fun bodyWidth(width: Int): Int = if (width < SMALL_BODY) SMALL_BODY else LARGE_BODY

        fun dialogWidth(width: Int): Int = bodyWidth(width) + WIDGET_PADDING
    }
}
