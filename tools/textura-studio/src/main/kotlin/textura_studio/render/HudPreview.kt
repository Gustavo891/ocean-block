package textura_studio.render

import one_block.textura.dialog.kit.IconScale
import one_block.textura.dialog.kit.UiArtCatalog
import org.bukkit.configuration.file.YamlConfiguration
import textura_studio.StudioLog
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.ceil

/** Região de `configs/hud/notificacoes.yml` já validada, com as mesmas regras do plugin. */
data class RegionSpec(
    val id: String,
    val anchor: Anchor,
    val x: Int,
    val y: Int,
    val lines: Int,
    val spacing: Int,
    val background: Boolean,
    val down: Boolean,
)

enum class Anchor(val yaml: String, val right: Boolean, val row: Int) {
    TOP_LEFT("TOPO_ESQUERDA", false, 0),
    TOP_RIGHT("TOPO_DIREITA", true, 0),
    MIDDLE_LEFT("MEIO_ESQUERDA", false, 1),
    MIDDLE_RIGHT("MEIO_DIREITA", true, 1),
    BOTTOM_LEFT("BASE_ESQUERDA", false, 2),
    BOTTOM_RIGHT("BASE_DIREITA", true, 2),
    ;

    companion object {
        fun of(raw: String?): Anchor? = entries.firstOrNull { it.yaml == raw?.trim()?.uppercase() }
    }
}

data class SampleLine(val text: String, val icon: String?)

data class HudSample(val main: String, val secondary: String?, val left: Boolean, val gap: Int)

data class HudRequest(
    val regionsYaml: String,
    val lines: Map<String, List<SampleLine>>,
    val hud: HudSample?,
    val monitorWidth: Int,
    val monitorHeight: Int,
    val scale: Int,
)

/** Área que a região ocupa na tela (todas as linhas possíveis) e o ponto de âncora + x/y. */
data class RegionBox(val id: String, val anchor: String, val x: Int, val y: Int, val w: Int, val h: Int, val pointX: Int, val pointY: Int)

data class HudResult(
    val image: BufferedImage,
    val guiWidth: Int,
    val guiHeight: Int,
    val scale: Int,
    val regions: List<RegionBox>,
    val hud: Box?,
    val warnings: List<String>,
)

/**
 * Desenha a tela de jogo com as notificações como o `text.vsh` do pack as posiciona
 * (âncora + x/y, passo 11 + espaçamento, placa de 11 px) e a HUD da bossbar.
 */
class HudPreview(private val assets: StudioAssets, private val plaqueFile: File) {

    fun render(request: HudRequest): HudResult {
        val log = StudioLog()
        val scale = Screen.scale(request.monitorWidth, request.monitorHeight, request.scale)
        val width = Screen.ceilDiv(request.monitorWidth, scale)
        val height = Screen.ceilDiv(request.monitorHeight, scale)
        val regions = parse(request.regionsYaml, log)

        val image = BufferedImage(request.monitorWidth, request.monitorHeight, BufferedImage.TYPE_INT_RGB)
        val boxes = mutableListOf<RegionBox>()
        var hudBox: Box? = null
        Canvas(image, scale).use { canvas ->
            world(canvas, width, height)
            val painter = TextPainter(canvas, assets.bodyFont, assets.vanillaFont)
            val rich = RichText(assets.lang, legacy = true)
            regions.forEach { region ->
                boxes += region(canvas, painter, rich, region, request.lines[region.id].orEmpty(), width, height)
            }
            request.hud?.let { hudBox = hud(canvas, it, width) }
        }
        return HudResult(image, width, height, scale, boxes, hudBox, assets.warnings + log.all())
    }

    // ---------- regiões ----------

    fun parse(yaml: String, log: StudioLog): List<RegionSpec> {
        val config = runCatching { YamlConfiguration().apply { loadFromString(yaml) } }.getOrElse {
            log.add("notificacoes.yml inválido: ${it.message}")
            return emptyList()
        }
        val seen = mutableSetOf<String>()
        return config.getKeys(false).mapNotNull { key ->
            val section = config.getConfigurationSection(key) ?: return@mapNotNull null.also { log.add("região '$key' não é uma seção; ignorada") }
            val id = section.getString("id")?.trim()?.takeIf { it.isNotEmpty() } ?: key
            val anchor = Anchor.of(section.getString("ancora"))
                ?: return@mapNotNull null.also { log.add("região '$id': ancora '${section.getString("ancora").orEmpty()}' inválida; ignorada") }
            if (!seen.add(id)) return@mapNotNull null.also { log.add("região '$id' repetida; mantida a primeira") }
            val direction = section.getString("direcao")?.trim()?.lowercase()
            val down = when (direction) {
                null -> anchor.row != 2
                "baixo" -> true
                "cima" -> false
                else -> (anchor.row != 2).also { log.add("região '$id': direcao '$direction' inválida") }
            }
            val draft = RegionSpec(
                id = id,
                anchor = anchor,
                x = section.getInt("x", 0).coerceIn(-MAX_OFFSET, MAX_OFFSET),
                y = section.getInt("y", 0).coerceIn(-MAX_OFFSET, MAX_OFFSET),
                lines = section.getInt("linhas", DEFAULT_LINES).coerceIn(1, MAX_LINES),
                spacing = section.getInt("espacamento", 1).coerceIn(0, MAX_SPACING),
                background = section.getBoolean("fundo", true),
                down = down,
            )
            val fitting = (0 until draft.lines).takeWhile { top(draft, it) != null }.count()
            when {
                fitting < 1 -> null.also { log.add("região '$id': y ${draft.y} sobe mais que o shader alcança; ignorada") }
                fitting < draft.lines -> draft.copy(lines = fitting).also { log.add("região '$id': só $fitting de ${draft.lines} linhas cabem no alcance do shader") }
                else -> draft
            }
        }
    }

    /** Topo do texto da linha relativo à âncora, ou null quando passa do alcance do shader. */
    private fun top(region: RegionSpec, index: Int): Int? {
        val pitch = LINE_HEIGHT + region.spacing
        val offset = if (region.down) region.y + index * pitch else region.y - index * pitch
        if (offset >= 0) return offset
        val lift = (-offset + LIFT_STEP - 1) / LIFT_STEP
        return if (lift > MAX_LIFT) null else offset
    }

    private fun origin(anchor: Anchor, width: Int, height: Int): Pair<Int, Int> {
        val x = if (anchor.right) width - MARGIN_X else MARGIN_X
        val y = when (anchor.row) {
            0 -> BAR_TEXT_TOP
            1 -> height / 2
            else -> height - MARGIN_BOTTOM - TEXT_LINE
        }
        return x to y
    }

    private fun region(
        canvas: Canvas,
        painter: TextPainter,
        rich: RichText,
        region: RegionSpec,
        lines: List<SampleLine>,
        width: Int,
        height: Int,
    ): RegionBox {
        val (ox, oy) = origin(region.anchor, width, height)
        val plaque = plaqueColor()
        var left = Int.MAX_VALUE
        var right = Int.MIN_VALUE

        lines.take(region.lines).forEachIndexed { index, line ->
            val top = top(region, index) ?: return@forEachIndexed
            val runs = rich.runs(line.text)
            val textWidth = runs.sumOf { run -> run.text.codePoints().toArray().sumOf { assets.kit.charAdvance(it) + if (run.bold) 1 else 0 } }
            val icon = line.icon?.takeIf { it.isNotBlank() }?.let(::icon)
            val content = (if (icon != null) ICON_BOX else 0) + textWidth
            val padding = if (region.background) PADDING_X else 0
            val boxWidth = content + 2 * padding
            val boxLeft = ox + if (region.anchor.right) region.x - boxWidth else region.x
            val textTop = oy + top
            left = minOf(left, boxLeft)
            right = maxOf(right, boxLeft + boxWidth)

            if (region.background && boxWidth >= 2) plaque(canvas, boxLeft, textTop - TEXT_TOP_PADDING, boxWidth, plaque)
            var x = boxLeft + padding
            if (icon != null) {
                canvas.draw(icon, x, textTop)
                x += ICON_BOX
            }
            runs.forEach { run ->
                painter.draw(run.text, x, textTop, run.rgb, run.bold, true) { assets.kit.charAdvance(it) }
                x += run.text.codePoints().toArray().sumOf { assets.kit.charAdvance(it) + if (run.bold) 1 else 0 }
            }
        }

        val tops = (0 until region.lines).mapNotNull { top(region, it) }
        val first = oy + tops.min() - TEXT_TOP_PADDING
        val last = oy + tops.max() - TEXT_TOP_PADDING + LINE_HEIGHT
        if (left == Int.MAX_VALUE) {
            left = ox + if (region.anchor.right) region.x - EMPTY_WIDTH else region.x
            right = left + EMPTY_WIDTH
        }
        return RegionBox(region.id, region.anchor.yaml, left, first, right - left, last - first, ox + region.x, oy + region.y)
    }

    private fun plaque(canvas: Canvas, x: Int, y: Int, width: Int, argb: Int) {
        canvas.fill(x, y + 1, 1, LINE_HEIGHT - 2, argb)
        canvas.fill(x + 1, y, width - 2, LINE_HEIGHT, argb)
        canvas.fill(x + width - 1, y + 1, 1, LINE_HEIGHT - 2, argb)
    }

    private fun plaqueColor(): Int {
        val image = plaqueFile.takeIf { it.isFile }?.let { runCatching { ImageIO.read(it) }.getOrNull() }
        return image?.getRGB(image.width / 2, image.height / 2)?.takeIf { it ushr 24 != 0 } ?: DEFAULT_PLAQUE
    }

    private fun icon(id: String): BufferedImage? {
        val normalized = if (id.startsWith(UiArtCatalog.ITEM_PREFIX, ignoreCase = true)) UiArtCatalog.ITEM_PREFIX + id.substring(5).lowercase() else id.trim()
        val art = assets.catalog.art(normalized)?.image ?: return null
        return IconScale.native(art, ICON_SIZE) ?: IconScale.resize(art, ICON_SIZE)
    }

    // ---------- HUD da bossbar ----------

    private fun hud(canvas: Canvas, sample: HudSample, width: Int): Box {
        val y = BAR_TEXT_TOP + BAR_PITCH
        val main = part(sample.main)
        val start = width / 2 - main.width / 2
        drawPart(canvas, main, start, y)
        var left = start
        var right = start + main.width
        sample.secondary?.takeIf { it.isNotBlank() }?.let { text ->
            val side = part(text)
            val x = if (sample.left) start - sample.gap - side.width else start + main.width + sample.gap
            drawPart(canvas, side, x, y)
            left = minOf(left, x)
            right = maxOf(right, x + side.width)
        }
        return Box(left, y - 3, right - left, HUD_GLYPH_HEIGHT + 4)
    }

    private class Part(val runs: List<TextRun>, val textWidth: Int, val segments: Int, val width: Int)

    private fun part(text: String): Part {
        val runs = RichText(assets.lang, legacy = false).runs(text)
        val textWidth = runs.sumOf { run -> run.text.codePoints().toArray().sumOf { assets.vanillaFont.advance(it) + if (run.bold) 1 else 0 } }
        val glyphs = HUD_GLYPHS.all { it in assets.glyphs }
        if (!glyphs) return Part(runs, textWidth, 0, textWidth)
        val segments = maxOf(2, ceil(textWidth / HUD_GLYPH_WIDTH.toDouble()).toInt()) + 2
        return Part(runs, textWidth, segments, segments * HUD_GLYPH_WIDTH)
    }

    private fun drawPart(canvas: Canvas, part: Part, x: Int, y: Int) {
        for (index in 0 until part.segments) {
            val id = when (index) {
                0 -> HUD_GLYPHS[0]
                part.segments - 1 -> HUD_GLYPHS[2]
                else -> HUD_GLYPHS[1]
            }
            val meta = assets.glyphs[id] ?: continue
            val image = assets.catalog.art(UiArtCatalog.GLYPH_PREFIX + id)?.image ?: continue
            val drawWidth = image.width * meta.height.toDouble() / image.height
            canvas.draw(image, (x + index * HUD_GLYPH_WIDTH).toDouble(), (y + TextPainter.BASE_ASCENT - meta.ascent).toDouble(), drawWidth, meta.height.toDouble())
        }
        val painter = TextPainter(canvas, assets.bossbarFont, assets.vanillaFont)
        var cursor = x + (part.width - part.textWidth) / 2
        part.runs.forEach { run ->
            painter.draw(run.text, cursor, y - BOSSBAR_FONT_SHIFT, run.rgb, run.bold, true)
            cursor += run.text.codePoints().toArray().sumOf { assets.vanillaFont.advance(it) + if (run.bold) 1 else 0 }
        }
    }

    // ---------- cenário ----------

    private fun world(canvas: Canvas, width: Int, height: Int) {
        canvas.gradient(SKY_TOP, SEA, width, height)
        assets.files.sprite("hud/crosshair")?.let { canvas.draw(it, (width - it.width) / 2, (height - it.height) / 2) }
        assets.files.sprite("hud/hotbar")?.let { hotbar ->
            canvas.draw(hotbar, (width - hotbar.width) / 2, height - hotbar.height)
            assets.files.sprite("hud/hotbar_selection")?.let { canvas.draw(it, (width - hotbar.width) / 2 - 1, height - hotbar.height - 1) }
        }
    }

    companion object {
        const val LINE_HEIGHT = 13
        const val TEXT_LINE = 9
        const val TEXT_TOP_PADDING = 3
        const val LIFT_STEP = 12
        const val MAX_LIFT = 15
        const val ICON_SIZE = 8
        const val ICON_BOX = 10
        const val PADDING_X = 5
        const val MARGIN_X = 2
        const val MARGIN_BOTTOM = 2
        const val BAR_TEXT_TOP = 3
        const val BAR_PITCH = 19
        const val MAX_LINES = 15
        const val MAX_SPACING = 32
        const val MAX_OFFSET = 4096
        const val DEFAULT_LINES = 3
        const val EMPTY_WIDTH = 80
        const val DEFAULT_PLAQUE = 0x42000000
        const val HUD_GLYPH_WIDTH = 6
        const val HUD_GLYPH_HEIGHT = 14
        const val BOSSBAR_FONT_SHIFT = 3
        val HUD_GLYPHS = listOf("hud_left", "hud_body", "hud_right")
        private const val SKY_TOP = 0x7BA4E0
        private const val SEA = 0x2E5B86
    }
}

/** Escala de GUI e tamanho da tela em pixels de GUI, com a regra do cliente. */
object Screen {
    private const val MIN_WIDTH = 320
    private const val MIN_HEIGHT = 240
    private const val MAX_AUTO = 16

    fun scale(width: Int, height: Int, requested: Int): Int {
        if (requested > 0) return requested
        var scale = 1
        while (scale < MAX_AUTO && width / (scale + 1) >= MIN_WIDTH && height / (scale + 1) >= MIN_HEIGHT) scale++
        return scale
    }

    fun ceilDiv(value: Int, by: Int): Int = (value + by - 1) / by
}
