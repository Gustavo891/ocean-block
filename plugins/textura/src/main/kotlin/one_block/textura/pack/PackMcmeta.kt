package one_block.textura.pack

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object PackMcmeta {

    private const val ENTRY = "pack.mcmeta"
    private const val FIRST_RANGED_FORMAT = 65
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    fun declareFormatRange(zip: File, packFormat: Int) {
        if (packFormat < FIRST_RANGED_FORMAT) return
        val temp = File(zip.parentFile, zip.name + ".tmp")
        ZipFile(zip).use { source ->
            ZipOutputStream(temp.outputStream().buffered()).use { out ->
                for (entry in source.entries()) {
                    val bytes = source.getInputStream(entry).use { it.readBytes() }
                    out.putNextEntry(ZipEntry(entry.name))
                    out.write(if (entry.name == ENTRY) ranged(bytes, packFormat) else bytes)
                    out.closeEntry()
                }
            }
        }
        if (!zip.delete() || !temp.renameTo(zip)) error("não foi possível substituir ${zip.name}")
    }

    private fun ranged(bytes: ByteArray, packFormat: Int): ByteArray {
        val root = runCatching { JsonParser.parseString(bytes.decodeToString()).asJsonObject }.getOrElse { JsonObject() }
        val pack = root.getAsJsonObject("pack") ?: JsonObject().also { root.add("pack", it) }
        pack.addProperty("pack_format", packFormat)
        pack.addProperty("min_format", packFormat)
        pack.addProperty("max_format", packFormat)
        return gson.toJson(root).toByteArray(Charsets.UTF_8)
    }
}
