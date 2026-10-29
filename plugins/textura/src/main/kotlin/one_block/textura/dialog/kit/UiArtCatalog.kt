package one_block.textura.dialog.kit

import java.awt.image.BufferedImage
import java.io.File
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import javax.imageio.ImageIO

data class ArtSlice(val left: Int, val right: Int, val top: Int, val bottom: Int, val nine: Boolean)

data class UiArt(val id: String, val image: BufferedImage, val slice: ArtSlice?)

class UiArtCatalog(
    private val uiRoot: File,
    private val glyphFile: (String) -> File?,
    private val logger: Logger,
    private val itemTexture: (String) -> BufferedImage? = { null },
    private val entityTexture: (String) -> BufferedImage? = { null },
) {

    private val cache = ConcurrentHashMap<String, Optional<UiArt>>()

    fun art(id: String): UiArt? = cache.computeIfAbsent(id) { Optional.ofNullable(load(it)) }.orElse(null)

    private fun load(id: String): UiArt? {
        if (id.startsWith(ITEM_PREFIX)) return itemTexture(id.removePrefix(ITEM_PREFIX))?.takeIf(::fits)?.let { UiArt(id, it, null) }
        if (id.startsWith(ENTITY_PREFIX)) return entityTexture(id.removePrefix(ENTITY_PREFIX))?.takeIf(::fits)?.let { UiArt(id, it, null) }
        val file = when {
            id.startsWith(UI_PREFIX) -> uiFile(id.removePrefix(UI_PREFIX))
            id.startsWith(GLYPH_PREFIX) -> glyphFile(id.removePrefix(GLYPH_PREFIX))
            else -> null
        } ?: return null

        val image = runCatching { ImageIO.read(file) }.getOrNull() ?: run {
            logger.warning("[ui] Imagem '$id' ilegível: ${file.path}")
            return null
        }
        if (!fits(image)) {
            logger.warning("[ui] Imagem '$id' tem ${image.width}x${image.height}; o máximo é ${MAX_WIDTH}x$MAX_HEIGHT")
            return null
        }
        return UiArt(id, image, readSlice(id, file, image))
    }

    private fun fits(image: BufferedImage): Boolean = image.width <= MAX_WIDTH && image.height <= MAX_HEIGHT

    private fun uiFile(path: String): File? {
        if (!NAME.matches(path)) return null
        return File(uiRoot, "$path.png").takeIf { it.isFile }
    }

    private fun readSlice(id: String, file: File, image: BufferedImage): ArtSlice? {
        val sliceFile = File(file.parentFile, file.name + ".slice")
        if (!sliceFile.isFile) return null

        val parts = sliceFile.readText().trim().split(Regex("\\s+")).mapNotNull { it.toIntOrNull() }
        val slice = when (parts.size) {
            2 -> ArtSlice(parts[0], parts[1], 0, 0, nine = false)
            4 -> ArtSlice(parts[0], parts[1], parts[2], parts[3], nine = true)
            else -> null
        }
        val valid = slice != null && parts.all { it >= 0 } &&
            slice.left + slice.right < image.width &&
            (!slice.nine || slice.top + slice.bottom < image.height)
        if (!valid) {
            logger.warning("[ui] '${sliceFile.name}' de '$id': esperado 'esquerda direita' ou 'esquerda direita topo base' com tampas menores que ${image.width}x${image.height}")
            return null
        }
        return slice
    }

    companion object {
        const val UI_PREFIX = "ui:"
        const val GLYPH_PREFIX = "glyph:"
        const val ITEM_PREFIX = "item:"
        const val ENTITY_PREFIX = "entidade:"
        const val MAX_WIDTH = 399
        const val MAX_HEIGHT = 256
        private val NAME = Regex("[a-z0-9_/-]+")
    }
}
