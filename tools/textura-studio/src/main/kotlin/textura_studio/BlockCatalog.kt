package textura_studio

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

data class TextureFace(val face: String, val path: String)

data class BlockEntry(val id: String, val name: String, val kind: String, val file: String, val faces: List<TextureFace>)

class BlockCatalog(private val data: File) {

    fun entries(): List<BlockEntry> =
        read("configs/blocks", "bloco", ::blockFaces) + read("configs/items", "item", ::itemFaces) + vanilla()

    private fun vanilla(): List<BlockEntry> {
        val dir = File(data, VANILLA_BLOCKS).takeIf { it.isDirectory } ?: return emptyList()
        return dir.listFiles { file -> file.isFile && file.extension == "png" }.orEmpty()
            .groupBy { file -> VANILLA_FACES.firstOrNull { file.nameWithoutExtension.endsWith("_$it") }?.let { file.nameWithoutExtension.removeSuffix("_$it") } ?: file.nameWithoutExtension }
            .toSortedMap()
            .map { (base, files) ->
                val faces = files.sortedBy { it.name }.map { file ->
                    TextureFace(file.nameWithoutExtension.removePrefix(base).removePrefix("_").ifEmpty { "all" }, "$VANILLA_BLOCKS/${file.name}")
                }
                BlockEntry(base, "$base (vanilla)", "vanilla", "minecraft", faces)
            }
    }

    fun write(files: StudioFiles, path: String, bytes: ByteArray) {
        if (entries().none { entry -> entry.faces.any { it.path == path } }) {
            throw StudioFileException("'$path' não é textura de nenhum bloco ou item do catálogo")
        }
        val image = runCatching { ImageIO.read(ByteArrayInputStream(bytes)) }.getOrNull() ?: throw StudioFileException("Arquivo não é um PNG legível")
        val current = File(data, path).takeIf { it.isFile }?.let { runCatching { ImageIO.read(it) }.getOrNull() }
        sizeProblem(current?.width, current?.height, image.width, image.height)?.let { throw StudioFileException(it) }
        files.writeImage(path, bytes)
    }

    private fun read(folder: String, kind: String, faces: (ConfigurationSection) -> List<TextureFace>): List<BlockEntry> {
        val dir = File(data, folder).takeIf { it.isDirectory } ?: return emptyList()
        return dir.listFiles { file -> file.isFile && file.extension == "yml" }.orEmpty().sortedBy { it.name }.flatMap { file ->
            val config = YamlConfiguration.loadConfiguration(file)
            config.getKeys(false).mapNotNull { key ->
                val section = config.getConfigurationSection(key) ?: return@mapNotNull null
                val list = faces(section).filter { File(data, it.path).isFile }
                list.takeIf { it.isNotEmpty() }?.let { BlockEntry(key, section.getString("name") ?: key, kind, file.name, it) }
            }
        }
    }

    private fun blockFaces(section: ConfigurationSection): List<TextureFace> {
        section.getString("texture")?.let { texture -> return listOfNotNull(face("all", texture)) }
        val textures = section.getConfigurationSection("textures") ?: return emptyList()
        return textures.getKeys(false).mapNotNull { name -> textures.getString(name)?.let { face(name, it) } }
    }

    private fun itemFaces(section: ConfigurationSection): List<TextureFace> {
        val texture = section.getString("texture") ?: return emptyList()
        if (!texture.contains("/textures/item/")) return emptyList()
        return listOfNotNull(face("item", texture))
    }

    private fun face(name: String, texture: String): TextureFace? =
        texture.takeIf { it.startsWith(ASSETS) && it.endsWith(".png") }?.let { TextureFace(name, "resource/$it") }

    companion object {
        private const val ASSETS = "assets/"
        private const val VANILLA_BLOCKS = "resource/assets/minecraft/textures/block"
        private val VANILLA_FACES = listOf("front_on", "front", "side", "top", "bottom")
        private const val DEFAULT_SIDE = 16
        private const val MAX_SIDE = 256

        fun sizeProblem(currentWidth: Int?, currentHeight: Int?, width: Int, height: Int): String? {
            if (width > MAX_SIDE || height > MAX_SIDE) return "A textura passa de ${MAX_SIDE}x$MAX_SIDE"
            if (width == DEFAULT_SIDE && height == DEFAULT_SIDE) return null
            if (currentWidth == null || currentHeight == null) return null
            if (width == currentWidth && height == currentHeight) return null
            return "Use ${DEFAULT_SIDE}x$DEFAULT_SIDE ou o tamanho atual (${currentWidth}x$currentHeight); veio ${width}x$height"
        }
    }
}
