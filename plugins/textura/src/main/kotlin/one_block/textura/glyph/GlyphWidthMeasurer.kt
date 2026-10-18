package one_block.textura.glyph

import java.io.File
import java.util.logging.Logger
import javax.imageio.ImageIO

/**
 * Mede a largura de um glyph a partir da imagem PNG: usa direto a largura em
 * pixels do arquivo. O `+1` de espaçamento entre caracteres não é incluído aqui
 * — quem soma é o [TextMetrics].
 */
object GlyphWidthMeasurer {

    /**
     * Largura (em px) da imagem [file], ou `null` se ela não puder ser lida (o
     * chamador decide o fallback).
     */
    fun measure(file: File, logger: Logger): Int? {
        if (!file.exists()) {
            logger.warning("Largura de '${file.name}' nao medida: arquivo nao encontrado. Declare 'size' no yml.")
            return null
        }
        val image = runCatching { ImageIO.read(file) }.getOrNull()
        if (image == null) {
            logger.warning("Largura de '${file.name}' nao medida: imagem ilegivel. Declare 'size' no yml.")
            return null
        }

        return image.width
    }
}
