package textura_studio.render

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import one_block.textura.api.DialogBinds
import one_block.textura.api.TexturaTags
import one_block.textura.dialog.DialogSession
import one_block.textura.dialog.json.ScreenCompiler
import one_block.textura.dialog.json.ScreenDefinition
import one_block.textura.dialog.json.ScreenParser
import one_block.textura.dialog.kit.ArtSlice
import one_block.textura.dialog.kit.ListSamples
import one_block.textura.dialog.kit.SurfaceComposer
import one_block.textura.dialog.layout.LaidOut
import one_block.textura.dialog.layout.UiLayout
import one_block.textura.dialog.render.TextMeasurer
import one_block.textura.dialog.render.UiRenderer
import one_block.textura.dialog.theme.UiTheme
import one_block.textura.registry.UiThemeRegistry
import textura_studio.StudioLog
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files

data class PreviewRequest(
    val id: String,
    val json: String,
    val page: String? = null,
    val monitorWidth: Int = 1920,
    val monitorHeight: Int = 1080,
    val scale: Int = 0,
    /** Entradas de exemplo por lista; listas ausentes usam `perPage` entradas vazias. */
    val lists: Map<String, List<Map<String, String>>> = emptyMap(),
    /** Valores de exemplo para `{chave}` fora de listas. */
    val values: Map<String, String> = emptyMap(),
    /** Temas ainda não salvos (nome do .yml -> conteúdo) que substituem os do disco no preview. */
    val themeFiles: Map<String, String> = emptyMap(),
    /** Imagens de exemplo para nós `pixels`; binds ausentes usam um xadrez do tamanho do nó. */
    val pixels: Map<String, BufferedImage> = emptyMap(),
)

data class Box(val x: Int, val y: Int, val w: Int, val h: Int)

data class BoxInfo(val tag: String, val x: Int, val y: Int, val w: Int, val h: Int)

data class HotspotInfo(val x: Int, val y: Int, val w: Int, val h: Int, val tooltip: String?, val clickable: Boolean)

data class PreviewResult(
    val image: BufferedImage?,
    val guiWidth: Int,
    val guiHeight: Int,
    val scale: Int,
    val body: Box?,
    val hotspots: List<HotspotInfo>,
    val boxes: List<BoxInfo>,
    val lists: Map<String, Int>,
    val pages: List<String>,
    val page: String?,
    val title: String?,
    val theme: String?,
    val contentWidth: Int?,
    val contentHeight: Int?,
    val warnings: List<String>,
    val error: String?,
)

/**
 * Monta a tela com o motor do Textura (parser → compilador → layout) e pinta como o
 * cliente mostra um dialog `notice`: cabeçalho com título, corpo centralizado e botão.
 */
class ScreenPreview(private val assets: StudioAssets) {

    private class Compiled(
        val definition: ScreenDefinition,
        val page: String?,
        val laid: LaidOut,
        val title: Component?,
        val lists: Map<String, Int>,
    )

    fun render(request: PreviewRequest): PreviewResult {
        val log = StudioLog()
        val scale = resolveScale(request)
        val guiWidth = ceilDiv(request.monitorWidth, scale)
        val guiHeight = ceilDiv(request.monitorHeight, scale)

        val definition = parse(request, log) ?: return failure(log, guiWidth, guiHeight, scale, log.all().lastOrNull() ?: "JSON inválido")
        val compiled = runCatching { compile(definition, request, log) }.getOrElse {
            return failure(log, guiWidth, guiHeight, scale, "Falha ao montar a tela: ${describe(it)}")
        } ?: return failure(log, guiWidth, guiHeight, scale, "Tema '${definition.theme}' não existe")

        val image = BufferedImage(request.monitorWidth, request.monitorHeight, BufferedImage.TYPE_INT_RGB)
        val placement = Canvas(image, scale).use { canvas -> paint(canvas, compiled, guiWidth, guiHeight, log) }

        return PreviewResult(
            image = image,
            guiWidth = guiWidth,
            guiHeight = guiHeight,
            scale = scale,
            body = placement.body,
            hotspots = placement.hotspots,
            boxes = placement.boxes,
            lists = compiled.lists,
            pages = definition.pages.keys.toList(),
            page = compiled.page,
            title = compiled.title?.let(PlainTextComponentSerializer.plainText()::serialize),
            theme = definition.theme,
            contentWidth = compiled.laid.width,
            contentHeight = compiled.laid.height,
            warnings = assets.warnings + log.all(),
            error = null,
        )
    }

    private fun failure(log: StudioLog, guiWidth: Int, guiHeight: Int, scale: Int, error: String) = PreviewResult(
        null, guiWidth, guiHeight, scale, null, emptyList(), emptyList(), emptyMap(), emptyList(), null, null, null, null, null, log.all(), error,
    )

    private fun parse(request: PreviewRequest, log: StudioLog): ScreenDefinition? {
        val folder = Files.createTempDirectory("textura-studio").toFile()
        return try {
            val file = File(folder, "${request.id}.json")
            file.writeText(request.json, Charsets.UTF_8)
            ScreenParser(log.logger).parse(file)
        } finally {
            folder.deleteRecursively()
        }
    }

    private fun compile(definition: ScreenDefinition, request: PreviewRequest, log: StudioLog): Compiled? {
        val theme = themes(request, log)[definition.theme] ?: return null
        val page = request.page?.takeIf { it in definition.pages } ?: definition.firstPage
        val limits = ListSamples.limits(definition) {}
        val binds = DialogBinds().apply {
            limits.forEach { (list, count) ->
                val entries = request.lists[list] ?: List(count) { emptyMap() }
                list(list) { entries }
            }
            PixelSamples.of(definition).forEach { (bind, sample) ->
                val image = request.pixels[bind] ?: sample
                pixels(bind) { image }
            }
        }
        val session = DialogSession(definition.id, binds, request.values).also {
            it.page = page
            it.defaults = definition.defaults
            it.choices = definition.choices
        }
        val compiler = ScreenCompiler(assets.kit) { log.add(it) }
        val screen = compiler.compile(definition, theme, session)
        val laid = UiLayout(assets.kit).layout(screen)
        return Compiled(definition, page, laid, compiler.title(definition, session), limits)
    }

    private fun themes(request: PreviewRequest, log: StudioLog): Map<String, UiTheme> {
        if (request.themeFiles.isEmpty()) return assets.themes
        val folder = Files.createTempDirectory("textura-studio-themes").toFile()
        return try {
            request.themeFiles.forEach { (name, text) -> File(folder, File(name).name).writeText(text, Charsets.UTF_8) }
            assets.themes + UiThemeRegistry(log.logger).also { it.load(folder) }.themes
        } finally {
            folder.deleteRecursively()
        }
    }

    private class Placement(val body: Box, val hotspots: List<HotspotInfo>, val boxes: List<BoxInfo>)

    private fun paint(canvas: Canvas, compiled: Compiled, width: Int, height: Int, log: StudioLog): Placement {
        background(canvas, width, height)
        header(canvas, compiled.title, width)
        footer(canvas, compiled.definition, width, height)
        return body(canvas, compiled.laid, width, height, log)
    }

    private fun background(canvas: Canvas, width: Int, height: Int) {
        canvas.gradient(WORLD_TOP, WORLD_BOTTOM, width, height)
        assets.files.guiTexture("inworld_menu_background")?.let { tile(canvas, it, 0, 0, width, height) }
        assets.files.guiTexture("inworld_header_separator")?.let { tile(canvas, it, 0, HEADER - it.height, width, it.height) }
        assets.files.guiTexture("inworld_footer_separator")?.let { tile(canvas, it, 0, height - FOOTER, width, it.height) }
    }

    private fun tile(canvas: Canvas, image: BufferedImage, x: Int, y: Int, width: Int, height: Int) {
        var ty = y
        while (ty < y + height) {
            var tx = x
            val h = minOf(image.height, y + height - ty)
            while (tx < x + width) {
                val w = minOf(image.width, x + width - tx)
                canvas.draw(image.getSubimage(0, 0, w, h), tx, ty)
                tx += image.width
            }
            ty += image.height
        }
    }

    private fun header(canvas: Canvas, title: Component?, width: Int) {
        val text = title?.let(PlainTextComponentSerializer.plainText()::serialize).orEmpty()
        val color = title?.color()?.value() ?: TextPainter.WHITE
        val painter = TextPainter(canvas, assets.vanillaFont, null)
        val textWidth = assets.vanillaFont.width(text)
        val rowWidth = textWidth + TITLE_GAP + WARNING_SIZE
        val rowX = (width - rowWidth) / 2
        val rowY = (HEADER - WARNING_SIZE) / 2
        painter.draw(text, rowX, rowY + (WARNING_SIZE - LINE) / 2, color, false, true)
        assets.files.sprite("dialog/warning_button")?.let { canvas.draw(it, rowX + textWidth + TITLE_GAP, rowY) }
    }

    private fun footer(canvas: Canvas, definition: ScreenDefinition, width: Int, height: Int) {
        val label = definition.close?.let { PlainTextComponentSerializer.plainText().serialize(TexturaTags.miniMessage.deserialize(it)) }
            ?: assets.translate("gui.ok", "OK")
        val x = (width - BUTTON_WIDTH) / 2
        val y = height - FOOTER + (FOOTER - BUTTON_HEIGHT) / 2
        assets.files.sprite("widget/button")?.let { sprite ->
            SurfaceComposer.compose(sprite, BUTTON_SLICE, BUTTON_WIDTH, BUTTON_HEIGHT)?.let { canvas.draw(it, x, y) }
        }
        val painter = TextPainter(canvas, assets.vanillaFont, null)
        val textWidth = assets.vanillaFont.width(label)
        painter.draw(label, x + (BUTTON_WIDTH - textWidth) / 2, y + (BUTTON_HEIGHT - 8) / 2, TextPainter.WHITE, false, true)
    }

    private fun body(canvas: Canvas, laid: LaidOut, width: Int, height: Int, log: StudioLog): Placement {
        val bodyWidth = UiRenderer.bodyWidth(laid.width)
        val widgetWidth = bodyWidth + WIDGET_PADDING * 2
        val lines = maxOf(1, (laid.height + LINE - 1) / LINE)
        val contentHeight = lines * LINE + WIDGET_PADDING * 2
        val maxHeight = (height - HEADER - FOOTER).coerceAtLeast(0)
        if (contentHeight > maxHeight) log.add("A tela tem ${laid.height} px de altura e não cabe nesta resolução: no jogo aparece barra de rolagem")
        val visible = minOf(contentHeight, maxHeight)
        val top = minOf(HEADER + CONTENT_MARGIN_TOP, height - FOOTER - visible)
        val widgetX = (width - widgetWidth) / 2
        val originX = widgetX + WIDGET_PADDING + (bodyWidth - laid.width) / 2
        val originY = top + WIDGET_PADDING

        val art = ArtPainter(canvas, assets.catalog) { log.add(it) }
        val text = TextPainter(canvas, assets.bodyFont, assets.vanillaFont)
        val measurer = TextMeasurer(assets.kit)
        val imagesByLine = laid.images.groupBy { it.y / LINE }
        val textsByLine = laid.texts.groupBy { it.y / LINE }
        val pixelsByLine = laid.pixels.groupBy { it.y / LINE }

        canvas.clip(0, top, width, visible)
        for (line in 0 until lines) {
            imagesByLine[line].orEmpty().forEach { art.draw(it, originX, originY, laid.width) }
            pixelsByLine[line].orEmpty().forEach { placed ->
                PixelSamples.draw(canvas, placed, originX + placed.x, originY + placed.y)
            }
            textsByLine[line].orEmpty().sortedBy { it.x }.forEach { placed ->
                val room = minOf(placed.maxWidth, laid.width - placed.x)
                val content = if (room > 0) measurer.fit(placed.text, placed.bold, room, placed.font) else ""
                if (content.isEmpty() && placed.text.isNotEmpty()) log.add("Texto '${placed.text}' não cabe em $room px e foi descartado")
                if (content.isNotEmpty() && content != placed.text) log.add("Texto '${placed.text}' cortado para caber em $room px")
                val rgb = placed.color?.value() ?: TextPainter.WHITE
                val painter = placed.font?.let { assets.extraFonts[it] }?.let { TextPainter(canvas, it, assets.vanillaFont) } ?: text
                painter.draw(content, originX + placed.x, originY + placed.y, rgb, placed.bold, placed.shadow) { assets.kit.charAdvance(it, placed.font) }
            }
        }
        canvas.unclip()

        val hotspots = laid.hotspots.filter { it.action != null || it.tooltip != null }.map {
            HotspotInfo(originX + it.x, originY + it.y, it.w, it.h, tooltip(it.tooltip), it.action != null)
        }
        val boxes = laid.boxes.map { BoxInfo(it.tag, originX + it.x, originY + it.y, it.w, it.h) }
        return Placement(Box(originX, originY, laid.width, laid.height), hotspots, boxes)
    }

    private fun tooltip(source: net.kyori.adventure.text.event.HoverEventSource<*>?): String? {
        val value = source?.asHoverEvent()?.value() as? Component ?: return null
        return PlainTextComponentSerializer.plainText().serialize(value)
    }

    private fun resolveScale(request: PreviewRequest): Int = Screen.scale(request.monitorWidth, request.monitorHeight, request.scale)

    private fun describe(error: Throwable): String {
        val root = generateSequence(error) { it.cause }.last()
        val at = root.stackTrace.firstOrNull { it.className.startsWith("one_block") }
            ?.let { " em ${it.className.substringAfterLast('.')}:${it.lineNumber}" }
            .orEmpty()
        return "${root.javaClass.simpleName}: ${root.message.orEmpty()}$at"
    }

    private fun ceilDiv(value: Int, by: Int): Int = (value + by - 1) / by

    companion object {
        const val HEADER = 33
        const val FOOTER = 33
        const val CONTENT_MARGIN_TOP = 30
        const val LINE = 9
        const val WIDGET_PADDING = 4
        const val TITLE_GAP = 10
        const val WARNING_SIZE = 20
        const val BUTTON_WIDTH = 150
        const val BUTTON_HEIGHT = 20
        const val MIN_GUI_WIDTH = 320
        const val MIN_GUI_HEIGHT = 240
        const val MAX_AUTO_SCALE = 16
        private const val WORLD_TOP = 0x4A6F9E
        private const val WORLD_BOTTOM = 0x1F3550
        private val BUTTON_SLICE = ArtSlice(3, 3, 3, 3, nine = true)
    }
}
