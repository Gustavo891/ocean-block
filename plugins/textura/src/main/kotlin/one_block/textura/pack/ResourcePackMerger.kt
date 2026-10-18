package one_block.textura.pack

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.base.Writable
import team.unnamed.creative.resources.MergeStrategy
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackReader
import java.io.File
import java.util.logging.Logger
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Mescla resource packs externos (pasta `external/`) sobre o pack principal gerado,
 * usando a API nativa de merge do creative ([ResourcePack.merge]).
 *
 * Cada entrada de `external/` pode ser:
 *  - um arquivo `.zip` de resource pack; ou
 *  - uma subpasta descompactada contendo `pack.mcmeta`/`assets`.
 *
 * Estrategia: [MergeStrategy.mergeAndKeepFirstOnError] — recursos combinaveis (como
 * fontes) tem seus providers unidos, preservando os glyphs do pack principal; em
 * conflitos irreconciliaveis o recurso do pack principal e mantido.
 */
class ResourcePackMerger(
    private val externalFolder: File,
    private val logger: Logger,
    private val packFormat: Int = PackBuilder.DEFAULT_PACK_FORMAT,
    private val strategy: MergeStrategy = MergeStrategy.mergeAndKeepFirstOnError()
) {
    private val reader = MinecraftResourcePackReader.minecraft()

    /** Mescla todos os packs de `external/` em [target] e retorna quantos foram aplicados. */
    fun mergeInto(target: ResourcePack): Int {
        if (!externalFolder.exists()) {
            externalFolder.mkdirs()
            return 0
        }

        val sources = externalFolder.listFiles()?.sortedBy { it.name } ?: return 0
        var merged = 0

        for (source in sources) {
            val isZip = source.isFile && source.extension.equals("zip", ignoreCase = true)
            val isDir = source.isDirectory && isResourcePackDir(source)
            if (!isZip && !isDir) continue

            if (mergeSource(source, isZip, target)) {
                merged++
            }
        }
        return merged
    }

    /**
     * Tenta o merge semantico nativo (preserva uniao de providers de fonte). Se o creative
     * falhar ao parsear o pack (ex.: model com transform desconhecido em outra versao do MC),
     * cai para um overlay de arquivos crus, copiando cada entrada verbatim.
     */
    private fun mergeSource(source: File, isZip: Boolean, target: ResourcePack): Boolean {
        // Pasta descompactada: mesmo papel do resource/ (cópia verbatim). Usa overlay
        // cru com skip-se-já-existe para não colidir com o que o resource/ já colocou —
        // o merge semântico reintroduziria o arquivo como recurso tipado e o writer
        // acusaria "already exists" no zip.
        if (!isZip) return rawOverlay(source, isZip = false, target = target)

        val flattened = flattenOverlays(source)
        val zip = flattened ?: source

        return try {
            val external = reader.readFromZipFile(zip)
            target.merge(external, strategy)
            logger.info("Resource pack externo mesclado: ${source.name}")
            true
        } catch (e: Exception) {
            logger.warning("Parse estrito de '${source.name}' falhou (${e.message}); aplicando overlay de arquivos crus.")
            rawOverlay(zip, isZip, target)
        } finally {
            flattened?.delete()
        }
    }

    /**
     * O `pack.mcmeta` do pack principal é escrito do zero, então as `overlays` de um
     * pack externo se perderiam e as pastas de overlay (`modelengine_1_21_6/`, etc.)
     * ficariam no zip sem ninguém para lê-las — o cliente usaria só os arquivos base,
     * de uma versão antiga. Aqui as overlays aplicáveis ao [packFormat] são achatadas
     * na raiz antes do merge, na ordem declarada, e as pastas somem.
     *
     * Retorna `null` quando não há overlay aplicável — aí o zip original serve.
     */
    private fun flattenOverlays(source: File): File? {
        val directories = try {
            ZipFile(source).use { zip ->
                val meta = zip.getEntry("pack.mcmeta") ?: return null
                val json = zip.getInputStream(meta).use { it.readBytes().decodeToString() }
                overlayDirectories(JsonParser.parseString(json).asJsonObject)
            }
        } catch (e: Exception) {
            logger.warning("Não consegui ler as overlays de '${source.name}': ${e.message}")
            return null
        }

        val stripShaders = dropShaders && hasShaders(source)
        if ((directories.all.isEmpty() || directories.applicable.isEmpty()) && !stripShaders) return null

        return try {
            val files = linkedMapOf<String, ByteArray>()

            ZipFile(source).use { zip ->
                for (entry in zip.entries()) {
                    if (entry.isDirectory) continue
                    if (directories.all.any { entry.name.startsWith("$it/") }) continue
                    if (skipShader(entry.name)) continue
                    files[entry.name] = zip.getInputStream(entry).use { it.readBytes() }
                }

                for (directory in directories.applicable) {
                    for (entry in zip.entries()) {
                        if (entry.isDirectory || !entry.name.startsWith("$directory/")) continue
                        val path = entry.name.removePrefix("$directory/")
                        if (skipShader(path)) continue
                        files[path] = zip.getInputStream(entry).use { it.readBytes() }
                    }
                }
            }

            val temp = File.createTempFile("textura-external-", ".zip")
            ZipOutputStream(temp.outputStream().buffered()).use { out ->
                files.forEach { (path, bytes) ->
                    out.putNextEntry(ZipEntry(path))
                    out.write(bytes)
                    out.closeEntry()
                }
            }

            if (directories.applicable.isNotEmpty()) logger.info("Overlays aplicadas em '${source.name}': ${directories.applicable.joinToString(", ")}")
            if (stripShaders) logger.warning("Shaders de '${source.name}' ignorados: o formato $packFormat não compila shaders antigos")
            temp
        } catch (e: Exception) {
            logger.warning("Falha ao achatar as overlays de '${source.name}': ${e.message}")
            null
        }
    }

    private fun overlayDirectories(meta: JsonObject): Overlays {
        val entries = meta.getAsJsonObject("overlays")?.getAsJsonArray("entries")
            ?: return Overlays(emptyList(), emptyList())

        val all = mutableListOf<String>()
        val applicable = mutableListOf<String>()

        for (element in entries) {
            val entry = element.asJsonObject
            val directory = entry.get("directory")?.asString ?: continue
            all += directory
            if (packFormat in formatRange(entry)) applicable += directory
        }

        return Overlays(all, applicable)
    }

    private fun formatRange(entry: JsonObject): IntRange {
        val formats = entry.get("formats")
        if (formats != null && formats.isJsonObject) {
            val range = formats.asJsonObject
            val min = range.get("min_inclusive")?.asInt ?: Int.MIN_VALUE
            val max = range.get("max_inclusive")?.asInt ?: Int.MAX_VALUE
            return min..max
        }

        if (formats != null && formats.isJsonPrimitive) {
            val exact = formats.asInt
            return exact..exact
        }

        if (formats != null && formats.isJsonArray) {
            val bounds = formats.asJsonArray
            val min = bounds.firstOrNull()?.let(::majorFormat) ?: Int.MIN_VALUE
            val max = bounds.lastOrNull()?.let(::majorFormat) ?: Int.MAX_VALUE
            return min..max
        }

        val min = entry.get("min_format")?.let(::majorFormat) ?: Int.MIN_VALUE
        val max = entry.get("max_format")?.let(::majorFormat) ?: Int.MAX_VALUE
        return min..max
    }

    private fun majorFormat(element: com.google.gson.JsonElement): Int? = when {
        element.isJsonPrimitive -> runCatching { element.asInt }.getOrNull()
        element.isJsonArray -> element.asJsonArray.firstOrNull()?.let { runCatching { it.asInt }.getOrNull() }
        else -> null
    }

    private data class Overlays(val all: List<String>, val applicable: List<String>)

    /** Copia cada arquivo do pack externo como recurso "desconhecido" (verbatim), sem parsing semantico. */
    private fun rawOverlay(source: File, isZip: Boolean, target: ResourcePack): Boolean {
        return try {
            if (isZip) {
                ZipFile(source).use { zip ->
                    for (entry in zip.entries()) {
                        if (entry.isDirectory || !shouldOverlay(entry.name)) continue
                        if (target.unknownFile(entry.name) != null) continue
                        val bytes = zip.getInputStream(entry).use { it.readBytes() }
                        target.unknownFile(entry.name, Writable.bytes(bytes))
                    }
                }
            } else {
                val root = source.toPath()
                source.walkTopDown().filter { it.isFile }.forEach { file ->
                    val path = root.relativize(file.toPath()).toString().replace('\\', '/')
                    if (shouldOverlay(path) && target.unknownFile(path) == null) {
                        target.unknownFile(path, Writable.bytes(file.readBytes()))
                    }
                }
            }
            logger.info("Resource pack externo mesclado via overlay: ${source.name}")
            true
        } catch (e: Exception) {
            logger.warning("Falha ao aplicar overlay de '${source.name}': ${e.message}")
            false
        }
    }

    /** Nao sobrescreve metadados do pack principal nem reintroduz o pack.mcmeta externo. */
    private fun shouldOverlay(path: String): Boolean =
        path != "pack.mcmeta" && path != "pack.png" && !skipShader(path)

    private val dropShaders = packFormat >= FIRST_SPIRV_FORMAT

    private fun skipShader(path: String): Boolean = dropShaders && SHADER_PATH.matches(path)

    private fun hasShaders(source: File): Boolean =
        runCatching { ZipFile(source).use { zip -> zip.entries().asSequence().any { SHADER_PATH.matches(it.name) } } }.getOrDefault(false)

    private fun isResourcePackDir(dir: File): Boolean =
        File(dir, "pack.mcmeta").exists() || File(dir, "assets").isDirectory

    private companion object {
        const val FIRST_SPIRV_FORMAT = 97
        val SHADER_PATH = Regex("^(?:[^/]+/)?assets/[^/]+/shaders/.+")
    }
}
