package textura_studio.render

import one_block.textura.dialog.json.DefContainer
import one_block.textura.dialog.json.DefNode
import one_block.textura.dialog.json.DefPixels
import one_block.textura.dialog.json.DefRepeat
import one_block.textura.dialog.json.DefScoped
import one_block.textura.dialog.json.ScreenDefinition
import one_block.textura.dialog.layout.PlacedPixels
import java.awt.image.BufferedImage

object PixelSamples {

    private const val LIGHT = 0xFF8A8A8A.toInt()
    private const val DARK = 0xFF5A5A5A.toInt()
    private const val CHECKER = 4

    fun of(definition: ScreenDefinition): Map<String, BufferedImage> =
        definition.containers().flatMap(::collect).associate { it.bind to checker(it.w * it.detail / it.scale, it.h * it.detail / it.scale) }

    fun draw(canvas: Canvas, placed: PlacedPixels, x: Int, y: Int) {
        if (placed.detail <= 1) {
            scaled(placed)?.let { canvas.draw(it, x, y) }
            return
        }
        val columns = minOf(placed.image.width, placed.width * placed.detail)
        val rows = minOf(placed.image.height, placed.height * placed.detail)
        if (columns <= 0 || rows <= 0) return
        val cropped = BufferedImage(columns, rows, BufferedImage.TYPE_INT_ARGB)
        for (cx in 0 until columns) for (cy in 0 until rows) {
            val argb = placed.image.getRGB(cx, cy)
            if ((argb ushr 24) >= OPAQUE) cropped.setRGB(cx, cy, argb or OPAQUE_MASK)
        }
        canvas.draw(cropped, x.toDouble(), y.toDouble(), columns.toDouble() / placed.detail, rows.toDouble() / placed.detail)
    }

    fun scaled(placed: PlacedPixels): BufferedImage? {
        val columns = minOf(placed.image.width, placed.width / placed.scale)
        val rows = minOf(placed.image.height, placed.height / placed.scale)
        if (columns <= 0 || rows <= 0) return null
        val result = BufferedImage(columns * placed.scale, rows * placed.scale, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until result.height) {
            for (x in 0 until result.width) {
                val argb = placed.image.getRGB(x / placed.scale, y / placed.scale)
                if ((argb ushr 24) >= OPAQUE) result.setRGB(x, y, argb or OPAQUE_MASK)
            }
        }
        return result
    }

    private fun collect(node: DefNode): List<DefPixels> = when (node) {
        is DefContainer -> node.children.flatMap(::collect)
        is DefRepeat -> collect(node.node)
        is DefScoped -> collect(node.node)
        is DefPixels -> listOf(node)
        else -> emptyList()
    }

    private fun checker(width: Int, height: Int): BufferedImage {
        val image = BufferedImage(width.coerceAtLeast(1), height.coerceAtLeast(1), BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                image.setRGB(x, y, if ((x / CHECKER + y / CHECKER) % 2 == 0) LIGHT else DARK)
            }
        }
        return image
    }

    private const val OPAQUE = 128
    private const val OPAQUE_MASK = 0xFF000000.toInt()
}
