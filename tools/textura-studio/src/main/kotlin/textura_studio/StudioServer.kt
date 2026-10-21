package textura_studio

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import textura_studio.render.PreviewRequest
import textura_studio.render.PreviewResult
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.util.Base64
import java.util.concurrent.Executors
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO

/** Servidor HTTP local (só 127.0.0.1): API do painel + arquivos do frontend. */
class StudioServer(private val studio: Studio, private val web: File?, private val port: Int) {

    private val gson: Gson = GsonBuilder().disableHtmlEscaping().serializeNulls().create()
    private val logger = Logger.getLogger("TexturaStudio")

    private val pack = PackRoutes(studio)

    fun start(): HttpServer {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", port), 0)
        server.executor = Executors.newFixedThreadPool(THREADS)
        server.createContext("/") { exchange ->
            try {
                route(exchange)
            } catch (error: HttpError) {
                json(exchange, error.status, mapOf("error" to error.message))
            } catch (error: InvalidDialogException) {
                json(exchange, 400, mapOf("error" to error.message))
            } catch (error: Exception) {
                logger.log(Level.SEVERE, "Erro em ${exchange.requestMethod} ${exchange.requestURI}", error)
                json(exchange, 500, mapOf("error" to (error.message ?: error.javaClass.simpleName)))
            } finally {
                exchange.close()
            }
        }
        server.start()
        return server
    }

    private fun route(exchange: HttpExchange) {
        val path = exchange.requestURI.path
        val method = exchange.requestMethod
        // Só aceita quem abriu o painel por 127.0.0.1/localhost (barra DNS rebinding de sites externos).
        val host = exchange.requestHeaders.getFirst("Host")?.substringBefore(':')
        if (host !in LOCAL_HOSTS) throw HttpError(403, "Host não permitido")
        if (!path.startsWith("/api/")) return static(exchange, path)

        val parts = path.removePrefix("/api/").split('/').filter { it.isNotEmpty() }.map { URLDecoder.decode(it, Charsets.UTF_8) }
        if (parts.isNotEmpty() && pack.handles(parts[0])) return pack.handle(exchange, method, parts)
        when {
            method == "GET" && parts == listOf("dialogs") -> json(exchange, 200, studio.store.list())
            method == "GET" && parts.size == 2 && parts[0] == "dialogs" -> readDialog(exchange, parts[1])
            method == "PUT" && parts.size == 2 && parts[0] == "dialogs" -> saveDialog(exchange, parts[1])
            method == "POST" && parts == listOf("preview") -> preview(exchange)
            method == "GET" && parts.size == 2 && parts[0] == "thumb" -> thumb(exchange, parts[1])
            method == "GET" && parts == listOf("themes") -> json(exchange, 200, studio.themes())
            method == "GET" && parts == listOf("assets") -> json(exchange, 200, studio.catalog())
            method == "GET" && parts == listOf("art") -> art(exchange)
            method == "POST" && parts == listOf("reload") -> {
                studio.reload()
                json(exchange, 200, mapOf("warnings" to studio.assets().warnings))
            }
            method == "GET" && parts == listOf("info") -> json(exchange, 200, mapOf("data" to studio.data.absolutePath))
            else -> throw HttpError(404, "Rota não encontrada: $method $path")
        }
    }

    private fun readDialog(exchange: HttpExchange, id: String) {
        val text = studio.store.read(id) ?: throw HttpError(404, "Tela '$id' não existe")
        send(exchange, 200, "application/json; charset=utf-8", text.toByteArray(Charsets.UTF_8))
    }

    private fun saveDialog(exchange: HttpExchange, id: String) {
        val text = exchange.requestBody.readBytes().decodeToString()
        studio.store.write(id, text)
        val result = studio.preview(PreviewRequest(id, text))
        json(exchange, 200, mapOf("saved" to "configs/ui/$id.json", "warnings" to result.warnings, "error" to result.error))
    }

    private fun preview(exchange: HttpExchange) {
        val body = runCatching { JsonParser.parseString(exchange.requestBody.readBytes().decodeToString()).asJsonObject }
            .getOrElse { throw HttpError(400, "Corpo do preview precisa ser JSON") }
        val request = PreviewRequest(
            id = body.string("id")?.takeIf { DialogStore.ID.matches(it) } ?: "preview",
            json = body.string("json") ?: throw HttpError(400, "Falta 'json'"),
            page = body.string("page"),
            monitorWidth = body.int("monitorWidth", 1920).coerceIn(320, 7680),
            monitorHeight = body.int("monitorHeight", 1080).coerceIn(240, 4320),
            scale = body.int("scale", 0).coerceIn(0, 16),
            lists = body.lists("lists"),
            values = body.strings(body.getAsJsonObject("values")),
            themeFiles = body.strings(body.getAsJsonObject("themeFiles")),
        )
        json(exchange, 200, payload(studio.preview(request)))
    }

    private fun thumb(exchange: HttpExchange, id: String) {
        val text = studio.store.read(id) ?: throw HttpError(404, "Tela '$id' não existe")
        val result = studio.preview(PreviewRequest(id, text, monitorWidth = THUMB_WIDTH, monitorHeight = THUMB_HEIGHT, scale = THUMB_SCALE))
        val image = result.image ?: throw HttpError(422, result.error ?: "Tela inválida")
        send(exchange, 200, "image/png", png(image), cache = false)
    }

    private fun art(exchange: HttpExchange) {
        val query = exchange.requestURI.rawQuery.orEmpty().split('&').mapNotNull { pair ->
            pair.split('=', limit = 2).takeIf { it.size == 2 }?.let { URLDecoder.decode(it[0], Charsets.UTF_8) to URLDecoder.decode(it[1], Charsets.UTF_8) }
        }.toMap()
        val id = query["id"] ?: throw HttpError(400, "Falta 'id'")
        val image = studio.art(id, query["theme"]) ?: throw HttpError(404, "Arte '$id' não existe")
        send(exchange, 200, "image/png", png(image), cache = false)
    }

    private fun payload(result: PreviewResult): Map<String, Any?> = mapOf(
        "png" to result.image?.let { Base64.getEncoder().encodeToString(png(it)) },
        "width" to result.image?.width,
        "height" to result.image?.height,
        "guiWidth" to result.guiWidth,
        "guiHeight" to result.guiHeight,
        "scale" to result.scale,
        "body" to result.body,
        "hotspots" to result.hotspots,
        "boxes" to result.boxes,
        "lists" to result.lists,
        "pages" to result.pages,
        "page" to result.page,
        "title" to result.title,
        "theme" to result.theme,
        "contentWidth" to result.contentWidth,
        "contentHeight" to result.contentHeight,
        "warnings" to result.warnings,
        "error" to result.error,
    )

    private fun static(exchange: HttpExchange, path: String) {
        val root = web?.takeIf { it.isDirectory } ?: run {
            send(exchange, 503, "text/plain; charset=utf-8", "Frontend não compilado (rode :tools:textura-studio:run)".toByteArray())
            return
        }
        val requested = File(root, path.removePrefix("/")).canonicalFile
        val inside = requested.toPath().startsWith(root.canonicalFile.toPath())
        val file = requested.takeIf { it.isFile && inside } ?: File(root, "index.html")
        send(exchange, 200, contentType(file.name), file.readBytes(), cache = file.name != "index.html")
    }

    private fun contentType(name: String): String = when (name.substringAfterLast('.', "")) {
        "html" -> "text/html; charset=utf-8"
        "js" -> "text/javascript; charset=utf-8"
        "css" -> "text/css; charset=utf-8"
        "svg" -> "image/svg+xml"
        "png" -> "image/png"
        "json" -> "application/json; charset=utf-8"
        "woff2" -> "font/woff2"
        else -> "application/octet-stream"
    }

    private fun json(exchange: HttpExchange, status: Int, value: Any?) =
        send(exchange, status, "application/json; charset=utf-8", gson.toJson(value).toByteArray(Charsets.UTF_8))

    private fun send(exchange: HttpExchange, status: Int, type: String, bytes: ByteArray, cache: Boolean = false) {
        exchange.responseHeaders.set("Content-Type", type)
        exchange.responseHeaders.set("Cache-Control", if (cache) "max-age=3600" else "no-store")
        exchange.sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
        if (bytes.isNotEmpty()) exchange.responseBody.use { it.write(bytes) }
    }

    private fun png(image: java.awt.image.BufferedImage): ByteArray =
        ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()

    private fun JsonObject.string(key: String): String? = get(key)?.takeIf { it.isJsonPrimitive }?.asString

    private fun JsonObject.strings(obj: JsonObject?): Map<String, String> =
        obj?.entrySet()?.filter { it.value.isJsonPrimitive }?.associate { it.key to it.value.asString }.orEmpty()

    private fun JsonObject.lists(key: String): Map<String, List<Map<String, String>>> =
        getAsJsonObject(key)?.entrySet()?.filter { it.value.isJsonArray }?.associate { (list, entries) ->
            list to entries.asJsonArray.filter { it.isJsonObject }.map { strings(it.asJsonObject) }
        }.orEmpty()

    private fun JsonObject.int(key: String, fallback: Int): Int =
        get(key)?.takeIf { it.isJsonPrimitive }?.let { runCatching { it.asInt }.getOrNull() } ?: fallback

    private companion object {
        const val THREADS = 4
        const val THUMB_WIDTH = 960
        const val THUMB_HEIGHT = 540
        const val THUMB_SCALE = 2
        val LOCAL_HOSTS = setOf("127.0.0.1", "localhost")
    }
}
