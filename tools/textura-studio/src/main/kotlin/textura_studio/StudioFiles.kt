package textura_studio

import org.bukkit.configuration.file.YamlConfiguration
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.imageio.ImageIO

class StudioFileException(message: String) : RuntimeException(message)

/**
 * Leitura e escrita dos arquivos que o painel edita, sempre dentro da pasta do Textura
 * e só nos caminhos liberados — `config.yml` (que guarda a chave do LobFile) e o resto
 * da pasta ficam fora.
 */
class StudioFiles(private val data: File) {

    private val root = data.canonicalFile

    fun list(folder: String, extension: String): List<String> {
        val dir = resolve(folder, directory = true)
        return dir.walkTopDown()
            .filter { it.isFile && it.extension == extension && !it.name.startsWith(".") }
            .map { it.relativeTo(root).invariantSeparatorsPath }
            .sorted()
            .toList()
    }

    fun read(path: String): String? = resolve(path).takeIf { it.isFile }?.readText(Charsets.UTF_8)

    fun bytes(path: String): ByteArray? = resolve(path).takeIf { it.isFile }?.readBytes()

    fun writeText(path: String, text: String) {
        val file = resolve(path)
        if (!TEXT.any { it.matches(relative(file)) }) throw StudioFileException("Não é permitido gravar '$path'")
        if (file.extension == "yml") {
            runCatching { YamlConfiguration().loadFromString(text) }.getOrElse { throw StudioFileException("YAML inválido: ${it.message}") }
        }
        atomic(file, text.toByteArray(Charsets.UTF_8))
    }

    fun writeImage(path: String, bytes: ByteArray) {
        val file = resolve(path)
        if (!IMAGE.matches(relative(file))) throw StudioFileException("Imagens só entram em resource/assets/<ns>/textures/…/*.png")
        if (bytes.size > MAX_IMAGE_BYTES) throw StudioFileException("Imagem maior que ${MAX_IMAGE_BYTES / 1024} KB")
        val image = runCatching { ImageIO.read(ByteArrayInputStream(bytes)) }.getOrNull() ?: throw StudioFileException("Arquivo não é um PNG legível")
        if (image.width > MAX_SIDE || image.height > MAX_SIDE) throw StudioFileException("Imagem passa de ${MAX_SIDE}x$MAX_SIDE")
        atomic(file, bytes)
    }

    fun delete(path: String) {
        val file = resolve(path)
        if (!SLICE.matches(relative(file))) throw StudioFileException("Só arquivos .slice podem ser apagados pelo painel")
        file.delete()
    }

    private fun relative(file: File): String = file.relativeTo(root).invariantSeparatorsPath

    private fun resolve(path: String, directory: Boolean = false): File {
        if (path.isBlank() || path.contains('\u0000')) throw StudioFileException("Caminho vazio")
        val file = File(root, path).canonicalFile
        if (!file.toPath().startsWith(root.toPath()) || file == root && !directory) throw StudioFileException("Caminho fora da pasta do Textura")
        val rel = relative(file)
        if (!READABLE.any { rel == it || rel.startsWith("$it/") }) throw StudioFileException("Caminho '$path' não liberado no painel")
        return file
    }

    private fun atomic(file: File, bytes: ByteArray) {
        file.parentFile.mkdirs()
        val temp = File(file.parentFile, ".${file.name}.tmp")
        temp.writeBytes(bytes)
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private companion object {
        val READABLE = listOf("configs/hud", "configs/glyphs", "configs/ui", "configs/fonts", "resource/assets")
        val TEXT = listOf(
            Regex("configs/hud/[a-z0-9_-]+\\.yml"),
            Regex("configs/glyphs/[a-z0-9_-]+\\.yml"),
            Regex("configs/ui/[a-z0-9_-]+\\.yml"),
            Regex("resource/assets/[a-z0-9_.-]+/textures/[a-z0-9_/.-]+\\.png\\.slice"),
        )
        val SLICE = Regex("resource/assets/[a-z0-9_.-]+/textures/[a-z0-9_/.-]+\\.png\\.slice")
        val IMAGE = Regex("resource/assets/[a-z0-9_.-]+/textures/[a-z0-9_/.-]+\\.png")
        const val MAX_IMAGE_BYTES = 2 * 1024 * 1024
        const val MAX_SIDE = 1024
    }
}
