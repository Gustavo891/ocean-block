package textura_studio

import com.sun.net.httpserver.HttpExchange
import textura_studio.HttpIo.bool
import textura_studio.HttpIo.int
import textura_studio.HttpIo.string
import textura_studio.render.GlyphRequest
import textura_studio.render.HudRequest
import textura_studio.render.HudSample
import textura_studio.render.SampleLine
import java.io.File
import java.util.Base64

/**
 * Rotas das seções além dos dialogs: arquivos de config liberados, imagens de
 * `resource/`, preview das notificações/HUD e do glyph no chat.
 */
class PackRoutes(private val studio: Studio) {

    fun handles(head: String): Boolean = head in PREFIXES

    fun handle(exchange: HttpExchange, method: String, parts: List<String>) {
        when {
            method == "GET" && parts == listOf("files") -> listFiles(exchange)
            method == "GET" && parts == listOf("file") -> readFile(exchange)
            method == "PUT" && parts == listOf("file") -> writeFile(exchange)
            method == "DELETE" && parts == listOf("file") -> deleteFile(exchange)
            method == "GET" && parts == listOf("raw") -> raw(exchange)
            method == "POST" && parts == listOf("upload") -> upload(exchange)
            method == "POST" && parts == listOf("hud", "preview") -> hudPreview(exchange)
            method == "POST" && parts == listOf("glyph", "preview") -> glyphPreview(exchange)
            method == "GET" && parts == listOf("blocks") -> HttpIo.json(exchange, 200, studio.blocks.entries())
            method == "POST" && parts == listOf("blocks", "texture") -> blockTexture(exchange)
            else -> throw HttpError(404, "Rota não encontrada: $method /api/${parts.joinToString("/")}")
        }
    }

    private fun path(exchange: HttpExchange): String = HttpIo.query(exchange)["path"] ?: throw HttpError(400, "Falta 'path'")

    private fun listFiles(exchange: HttpExchange) {
        val query = HttpIo.query(exchange)
        val dir = query["dir"] ?: throw HttpError(400, "Falta 'dir'")
        HttpIo.json(exchange, 200, guard { studio.files.list(dir, query["ext"] ?: "yml") })
    }

    private fun readFile(exchange: HttpExchange) {
        val text = guard { studio.files.read(path(exchange)) } ?: throw HttpError(404, "Arquivo não existe")
        HttpIo.send(exchange, 200, "text/plain; charset=utf-8", text.toByteArray(Charsets.UTF_8))
    }

    private fun writeFile(exchange: HttpExchange) {
        val path = path(exchange)
        guard { studio.files.writeText(path, exchange.requestBody.readBytes().decodeToString()) }
        studio.reload()
        HttpIo.json(exchange, 200, mapOf("saved" to path))
    }

    private fun deleteFile(exchange: HttpExchange) {
        guard { studio.files.delete(path(exchange)) }
        studio.reload()
        HttpIo.json(exchange, 200, mapOf("deleted" to true))
    }

    private fun raw(exchange: HttpExchange) {
        val path = path(exchange)
        if (!path.endsWith(".png")) throw HttpError(400, "Só PNG")
        val bytes = guard { studio.files.bytes(path) } ?: throw HttpError(404, "Imagem não existe")
        HttpIo.send(exchange, 200, "image/png", bytes)
    }

    private fun upload(exchange: HttpExchange) {
        val path = path(exchange)
        guard { studio.files.writeImage(path, exchange.requestBody.readNBytes(MAX_UPLOAD)) }
        studio.reload()
        HttpIo.json(exchange, 200, mapOf("saved" to path))
    }

    private fun blockTexture(exchange: HttpExchange) {
        val path = path(exchange)
        guard { studio.blocks.write(studio.files, path, exchange.requestBody.readNBytes(MAX_UPLOAD)) }
        studio.reload()
        HttpIo.json(exchange, 200, mapOf("saved" to path))
    }

    private fun hudPreview(exchange: HttpExchange) {
        val body = HttpIo.body(exchange)
        val lines = body.getAsJsonObject("lines")?.entrySet()?.associate { (region, entries) ->
            region to entries.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { element ->
                element.takeIf { it.isJsonObject }?.asJsonObject?.let { SampleLine(it.string("text").orEmpty(), it.string("icon")) }
            }.orEmpty()
        }.orEmpty()
        val hud = body.getAsJsonObject("hud")?.takeIf { it.bool("enabled", true) }?.let {
            HudSample(it.string("main").orEmpty(), it.string("secondary"), it.string("side") == "left", it.int("gap", 6).coerceIn(0, 64))
        }
        val request = HudRequest(
            regionsYaml = body.string("regions").orEmpty(),
            lines = lines,
            hud = hud,
            monitorWidth = body.int("monitorWidth", 1920).coerceIn(320, 7680),
            monitorHeight = body.int("monitorHeight", 1080).coerceIn(240, 4320),
            scale = body.int("scale", 0).coerceIn(0, 16),
        )
        val result = studio.hudPreview(request)
        HttpIo.json(
            exchange,
            200,
            mapOf(
                "png" to Base64.getEncoder().encodeToString(HttpIo.png(result.image)),
                "guiWidth" to result.guiWidth,
                "guiHeight" to result.guiHeight,
                "scale" to result.scale,
                "regions" to result.regions,
                "hud" to result.hud,
                "warnings" to result.warnings,
            ),
        )
    }

    private fun glyphPreview(exchange: HttpExchange) {
        val body = HttpIo.body(exchange)
        val caminho = body.string("path") ?: throw HttpError(400, "Falta 'path'")
        val file = File(studio.data, "resource/$caminho").canonicalFile
        if (!file.toPath().startsWith(File(studio.data, "resource").canonicalFile.toPath()) || !file.isFile) throw HttpError(404, "Imagem '$caminho' não existe em resource/")
        val image = studio.glyphPreview(
            GlyphRequest(
                file = file,
                ascent = body.int("ascent", 8).coerceIn(-512, 512),
                height = body.int("height", 8).coerceIn(1, 256),
                size = body.get("size")?.takeIf { it.isJsonPrimitive }?.asInt,
                text = body.string("text") ?: "Texto {glyph} no chat",
            ),
        )
        HttpIo.json(exchange, 200, mapOf("png" to Base64.getEncoder().encodeToString(HttpIo.png(image))))
    }

    private fun <T> guard(block: () -> T): T = try {
        block()
    } catch (error: StudioFileException) {
        throw HttpError(400, error.message ?: "Arquivo recusado")
    }

    private companion object {
        val PREFIXES = setOf("files", "file", "raw", "upload", "hud", "glyph", "blocks")
        const val MAX_UPLOAD = 2 * 1024 * 1024 + 1
    }
}
