package textura_studio.render

import net.kyori.adventure.key.Key
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipFile
import javax.imageio.ImageIO

/**
 * Arquivos de assets procurados primeiro em `resource/` do Textura e depois no client
 * jar vanilla que o próprio Textura baixa em `generated/vanilla/`.
 */
class GameFiles(private val resourceRoot: File, val clientJar: File?) {

    private val images = ConcurrentHashMap<String, Any>()

    fun bytes(path: String): ByteArray? {
        File(resourceRoot, path).takeIf { it.isFile }?.let { return it.readBytes() }
        val jar = clientJar ?: return null
        return ZipFile(jar).use { zip -> zip.getEntry(path)?.let { entry -> zip.getInputStream(entry).use { it.readBytes() } } }
    }

    fun text(path: String): String? = bytes(path)?.decodeToString()

    fun image(path: String): BufferedImage? {
        val cached = images.computeIfAbsent(path) { key ->
            bytes(key)?.let { runCatching { ImageIO.read(it.inputStream()) }.getOrNull() } ?: MISSING
        }
        return cached as? BufferedImage
    }

    fun fontJson(id: Key): String? = text("assets/${id.namespace()}/font/${id.value()}.json")

    fun fontTexture(file: Key): BufferedImage? = image("assets/${file.namespace()}/textures/${file.value()}")

    /** Sprite de GUI vanilla, ex.: `widget/button` em `textures/gui/sprites/`. */
    fun sprite(name: String): BufferedImage? = image("assets/minecraft/textures/gui/sprites/$name.png")

    fun guiTexture(name: String): BufferedImage? = image("assets/minecraft/textures/gui/$name.png")

    /** Nomes dos arquivos do client jar sob `prefix` (sem a extensão). */
    fun jarNames(prefix: String, extension: String): List<String> {
        val jar = clientJar ?: return emptyList()
        return ZipFile(jar).use { zip ->
            zip.entries().asSequence()
                .map { it.name }
                .filter { it.startsWith(prefix) && it.endsWith(extension) && !it.substring(prefix.length).contains('/') }
                .map { it.substring(prefix.length).removeSuffix(extension) }
                .sorted()
                .toList()
        }
    }

    companion object {
        private val MISSING = Any()

        fun newestClientJar(vanillaFolder: File): File? =
            vanillaFolder.listFiles { file -> file.isFile && file.name.startsWith("client-") && file.name.endsWith(".jar") }
                ?.maxByOrNull { it.lastModified() }

        fun version(jar: File): String = jar.name.removePrefix("client-").removeSuffix(".jar")
    }
}
