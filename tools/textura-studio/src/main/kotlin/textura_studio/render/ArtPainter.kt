package textura_studio.render

import one_block.textura.dialog.kit.IconScale
import one_block.textura.dialog.kit.SurfaceComposer
import one_block.textura.dialog.kit.UiArtCatalog
import one_block.textura.dialog.kit.UiPieces
import one_block.textura.dialog.layout.ImageVariant
import one_block.textura.dialog.layout.PlacedImage
import java.awt.image.BufferedImage

/**
 * Desenha as imagens do layout do mesmo jeito que o pack gerado monta os glyphs:
 * tampas + miolos de 1, 2, 4… 256 px repetidos, e superfícies 9-slice compostas na altura.
 */
class ArtPainter(private val canvas: Canvas, private val catalog: UiArtCatalog, private val warn: (String) -> Unit) {

    private val middles = HashMap<String, BufferedImage>()

    fun draw(placed: PlacedImage, originX: Int, originY: Int, areaWidth: Int) {
        when (val variant = placed.variant) {
            ImageVariant.Plain -> plain(placed, originX, originY, areaWidth)
            is ImageVariant.Icon -> icon(placed, variant.size, originX, originY)
            ImageVariant.Surface -> surface(placed, originX, originY)
        }
    }

    private fun plain(placed: PlacedImage, originX: Int, originY: Int, areaWidth: Int) {
        val art = catalog.art(placed.id) ?: return warn("Imagem '${placed.id}' não existe")
        val slice = art.slice?.takeUnless { it.nine }
        val drawWidth = if (slice != null) placed.width else art.image.width
        if (placed.x < 0 || placed.x + drawWidth > areaWidth) {
            warn("Imagem '${placed.id}' em x=${placed.x} passa da largura $areaWidth e foi descartada")
            return
        }
        if (slice == null) {
            canvas.draw(art.image, originX + placed.x, originY + placed.y)
            return
        }
        sliced(placed.id, art.image, slice.left, slice.right, 1, placed, originX, originY)
    }

    private fun icon(placed: PlacedImage, size: Int, originX: Int, originY: Int) {
        val art = catalog.art(placed.id) ?: return warn("Ícone '${placed.id}' não existe")
        val native = if (placed.id.startsWith(UiArtCatalog.ITEM_PREFIX) && size < art.image.height) IconScale.native(art.image, size) else null
        val detail = if (native == null) IconScale.detail(art.image, size) else 1
        val image = native ?: IconScale.resize(art.image, size * detail) ?: return
        canvas.draw(image, (originX + placed.x).toDouble(), (originY + placed.y).toDouble(), image.width.toDouble() / detail, image.height.toDouble() / detail)
    }

    private fun surface(placed: PlacedImage, originX: Int, originY: Int) {
        val art = catalog.art(placed.id) ?: return warn("Surface '${placed.id}' não existe")
        val slice = art.slice?.takeIf { it.nine } ?: return warn("Surface '${placed.id}' precisa de .slice com 'esquerda direita topo base'")
        if (placed.height > UiArtCatalog.MAX_HEIGHT) return warn("Surface '${placed.id}' com ${placed.height} px de altura passa de ${UiArtCatalog.MAX_HEIGHT}")
        val strip = SurfaceComposer.compose(art.image, slice, art.image.width, placed.height * slice.detail)
            ?: return warn("Surface '${placed.id}' com ${placed.height} px é menor que as tampas")
        sliced("surface:${placed.id}@${placed.height}", strip, slice.left, slice.right, slice.detail, placed, originX, originY)
    }

    private fun sliced(name: String, image: BufferedImage, left: Int, right: Int, detail: Int, placed: PlacedImage, originX: Int, originY: Int) {
        val leftWidth = left / detail
        val rightWidth = right / detail
        if (placed.width < leftWidth + rightWidth) return warn("Imagem '${placed.id}' mais estreita que as tampas")
        val x0 = (originX + placed.x).toDouble()
        val y0 = (originY + placed.y).toDouble()
        val height = image.height.toDouble() / detail
        if (left > 0) canvas.draw(image.getSubimage(0, 0, left, image.height), x0, y0, leftWidth.toDouble(), height)

        var x = placed.x + leftWidth
        var remaining = placed.width - leftWidth - rightWidth
        val sizes = UiPieces.MIDDLE_SIZES.sortedDescending()
        while (remaining > 0) {
            val size = sizes.firstOrNull { it <= remaining } ?: break
            canvas.draw(middle(name, image, left, right, size * detail), (originX + x).toDouble(), y0, size.toDouble(), height)
            x += size
            remaining -= size
        }
        if (right > 0) {
            val cap = image.getSubimage(image.width - right, 0, right, image.height)
            canvas.draw(cap, x0 + placed.width - rightWidth, y0, rightWidth.toDouble(), height)
        }
    }

    private fun middle(name: String, image: BufferedImage, left: Int, right: Int, width: Int): BufferedImage =
        middles.getOrPut("$name#$width") { UiPieces.tiledMiddle(image, left, right, width) }
}
