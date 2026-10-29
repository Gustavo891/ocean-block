package one_block.textura.pack

import java.awt.image.BufferedImage
import java.io.File
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import javax.imageio.ImageIO

class IconLibrary(private val root: File, private val logger: Logger) {

    private val cache = ConcurrentHashMap<String, Optional<BufferedImage>>()

    fun item(name: String): BufferedImage? = read("items", name)

    fun entity(name: String): BufferedImage? = read("entities/isometric", name)

    fun entityFlat(name: String): BufferedImage? = read("entities/flat", name)

    private fun read(folder: String, name: String): BufferedImage? {
        if (!NAME.matches(name)) return null
        return cache.computeIfAbsent("$folder/$name") {
            val file = File(root, "$folder/${name.uppercase()}.png")
            if (!file.isFile) return@computeIfAbsent Optional.empty()
            val image = runCatching { ImageIO.read(file) }.getOrNull()
            if (image == null) logger.warning("[ui] Ícone da biblioteca ilegível: ${file.path}")
            Optional.ofNullable(image?.let { IconResample.toMax(it, MAX_SIZE) })
        }.orElse(null)
    }

    companion object {
        const val FOLDER = "library/mc-assets"
        const val MAX_SIZE = 128
        private val NAME = Regex("[a-z0-9_]+")
    }
}
