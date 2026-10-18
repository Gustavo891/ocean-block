package one_block.textura.glyph

import org.bukkit.configuration.file.YamlConfiguration
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.io.File

/** Resultado da importação de um glyph a partir de uma URL. */
sealed interface GlyphImportResult {
    data class Success(val id: String, val caminho: String, val unicode: String?) : GlyphImportResult
    data class Failure(val reason: String) : GlyphImportResult
}

/**
 * Baixa uma imagem `.png` de uma URL e a registra como glyph na pasta `glyphs/imported`,
 * gravando a entrada correspondente em `glyphs/imported.yml`.
 *
 * Faz apenas I/O de arquivos — não toca em estado do servidor nem nos registries. O fluxo
 * de reload/geração do pack fica a cargo do chamador.
 */
class GlyphImporter(private val glyphsFolder: File) {

    fun import(nome: String, unicode: String?, url: String): GlyphImportResult {
        if (!NAME_REGEX.matches(nome)) {
            return GlyphImportResult.Failure("Nome invalido. Use apenas letras minusculas, numeros, '_' ou '-' (max 32).")
        }

        val normalizedUnicode = if (unicode == null) {
            null
        } else {
            normalizeUnicode(unicode)
                ?: return GlyphImportResult.Failure("Unicode invalido. Use '\\uXXXX' (ate 4 digitos hex) ou um unico caractere.")
        }

        val parsedUrl = runCatching { URI(url).toURL() }.getOrNull()
            ?: return GlyphImportResult.Failure("URL invalida.")
        if (parsedUrl.protocol !in ALLOWED_SCHEMES) {
            return GlyphImportResult.Failure("Apenas URLs http/https sao aceitas.")
        }

        val bytes = runCatching { download(parsedUrl) }
            .getOrElse { return GlyphImportResult.Failure("Falha ao baixar a imagem: ${it.message}") }
        if (!isPng(bytes)) {
            return GlyphImportResult.Failure("O arquivo baixado nao e um PNG valido.")
        }

        val importedDir = File(glyphsFolder, IMPORTED_DIR).apply { mkdirs() }
        val pngFile = File(importedDir, "$nome.png")
        // Guarda defensiva contra path traversal (nome ja e validado pela regex).
        if (!pngFile.canonicalPath.startsWith(importedDir.canonicalFile.path + File.separator)) {
            return GlyphImportResult.Failure("Caminho de destino invalido.")
        }
        runCatching { pngFile.writeBytes(bytes) }
            .getOrElse { return GlyphImportResult.Failure("Falha ao salvar a imagem: ${it.message}") }

        val caminho = "$IMPORTED_DIR/$nome.png"
        writeYaml(nome, caminho, normalizedUnicode)
        return GlyphImportResult.Success(nome, caminho, normalizedUnicode)
    }

    private fun download(url: URL): ByteArray {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            requestMethod = "GET"
            instanceFollowRedirects = true
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("HTTP $code")
            conn.inputStream.use { input ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    total += read
                    if (total > MAX_BYTES) throw IOException("Imagem excede ${MAX_BYTES / 1024} KB.")
                    out.write(buffer, 0, read)
                }
                return out.toByteArray()
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun writeYaml(key: String, caminho: String, unicode: String?) {
        val yml = File(glyphsFolder, IMPORTED_YML)
        val config = YamlConfiguration.loadConfiguration(yml)
        config.set("$key.id", key)
        config.set("$key.caminho", caminho)
        config.set("$key.unicode", unicode ?: "")
        config.save(yml)
    }

    /** Aceita `\uXXXX` (1..4 digitos hex, BMP) ou um unico code point literal. */
    private fun normalizeUnicode(raw: String): String? {
        if (raw.startsWith("\\u")) {
            val hex = raw.substring(2)
            if (hex.isEmpty() || hex.length > 4) return null
            if (!hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
            return "\\u" + hex.uppercase()
        }
        return if (raw.isNotEmpty() && raw.codePointCount(0, raw.length) == 1) raw else null
    }

    private fun isPng(bytes: ByteArray): Boolean {
        if (bytes.size < PNG_SIGNATURE.size) return false
        return PNG_SIGNATURE.indices.all { bytes[it] == PNG_SIGNATURE[it] }
    }

    private companion object {
        val NAME_REGEX = Regex("^[a-z0-9_-]{1,32}$")
        val ALLOWED_SCHEMES = setOf("http", "https")
        val PNG_SIGNATURE = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        )
        const val IMPORTED_DIR = "imported"
        const val IMPORTED_YML = "imported.yml"
        const val MAX_BYTES = 1024 * 1024 // 1 MB
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 15_000
    }
}
