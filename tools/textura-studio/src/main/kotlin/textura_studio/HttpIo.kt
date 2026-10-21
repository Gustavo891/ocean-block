package textura_studio

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sun.net.httpserver.HttpExchange
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.URLDecoder
import javax.imageio.ImageIO

/** Erro que vira resposta HTTP com o status dado e `{error: mensagem}`. */
class HttpError(val status: Int, message: String) : RuntimeException(message)

object HttpIo {

    val gson: Gson = GsonBuilder().disableHtmlEscaping().serializeNulls().create()

    fun json(exchange: HttpExchange, status: Int, value: Any?) =
        send(exchange, status, "application/json; charset=utf-8", gson.toJson(value).toByteArray(Charsets.UTF_8))

    fun send(exchange: HttpExchange, status: Int, type: String, bytes: ByteArray, cache: Boolean = false) {
        exchange.responseHeaders.set("Content-Type", type)
        exchange.responseHeaders.set("Cache-Control", if (cache) "max-age=3600" else "no-store")
        exchange.sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
        if (bytes.isNotEmpty()) exchange.responseBody.use { it.write(bytes) }
    }

    fun png(image: BufferedImage): ByteArray = ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()

    fun query(exchange: HttpExchange): Map<String, String> =
        exchange.requestURI.rawQuery.orEmpty().split('&').mapNotNull { pair ->
            pair.split('=', limit = 2).takeIf { it.size == 2 }?.let { URLDecoder.decode(it[0], Charsets.UTF_8) to URLDecoder.decode(it[1], Charsets.UTF_8) }
        }.toMap()

    fun body(exchange: HttpExchange): JsonObject =
        runCatching { JsonParser.parseString(exchange.requestBody.readBytes().decodeToString()).asJsonObject }
            .getOrElse { throw HttpError(400, "Corpo precisa ser um objeto JSON") }

    fun JsonObject.string(key: String): String? = get(key)?.takeIf { it.isJsonPrimitive }?.asString

    fun JsonObject.int(key: String, fallback: Int): Int =
        get(key)?.takeIf { it.isJsonPrimitive }?.let { runCatching { it.asInt }.getOrNull() } ?: fallback

    fun JsonObject.bool(key: String, fallback: Boolean): Boolean =
        get(key)?.takeIf { it.isJsonPrimitive }?.let { runCatching { it.asBoolean }.getOrNull() } ?: fallback

    fun strings(obj: JsonObject?): Map<String, String> =
        obj?.entrySet()?.filter { it.value.isJsonPrimitive }?.associate { it.key to it.value.asString }.orEmpty()
}
