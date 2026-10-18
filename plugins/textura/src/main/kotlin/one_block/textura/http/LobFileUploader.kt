package one_block.textura.http

import com.google.gson.JsonParser
import java.io.DataOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URI

/** Sobe arquivos pro LobFile via API v3 (multipart, header X-API-Key). */
object LobFileUploader {

    private const val ENDPOINT = "https://lobfile.com/api/v3/upload"

    /** Envia [file] e retorna a URL direta. Lança em qualquer falha. */
    fun upload(file: File, apiKey: String): String {
        val boundary = "----TexturaBoundary${System.currentTimeMillis()}"
        val conn = (URI(ENDPOINT).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 120_000
            setRequestProperty("X-API-Key", apiKey)
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }

        DataOutputStream(conn.outputStream).use { out ->
            out.writeBytes("--$boundary\r\n")
            out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"${file.name}\"\r\n")
            out.writeBytes("Content-Type: application/zip\r\n\r\n")
            file.inputStream().use { it.copyTo(out) }
            out.writeBytes("\r\n--$boundary--\r\n")
            out.flush()
        }

        val code = conn.responseCode
        val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()

        val json = runCatching { JsonParser.parseString(body).asJsonObject }
            .getOrElse { throw IllegalStateException("LobFile: resposta invalida (HTTP $code): ${body.take(200)}") }

        val success = json.get("success")?.asBoolean ?: false
        if (!success) {
            val err = json.get("error")?.takeIf { !it.isJsonNull }?.asString ?: "HTTP $code"
            throw IllegalStateException("LobFile recusou: $err")
        }

        return json.get("url")?.takeIf { !it.isJsonNull }?.asString
            ?: throw IllegalStateException("LobFile: resposta sem campo 'url'")
    }
}
