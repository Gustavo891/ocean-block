package textura_studio

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

data class DialogSummary(val id: String, val title: String?, val theme: String?, val pages: Int, val modified: Long)

class InvalidDialogException(message: String) : RuntimeException(message)

/** Lê e grava as telas em `configs/ui/<id>.json` do Textura. */
class DialogStore(private val folder: File) {

    fun list(): List<DialogSummary> =
        folder.listFiles { file -> file.isFile && file.extension == "json" }.orEmpty()
            .sortedBy { it.name }
            .map(::summary)

    fun read(id: String): String? = file(id).takeIf { it.isFile }?.readText(Charsets.UTF_8)

    fun exists(id: String): Boolean = file(id).isFile

    fun write(id: String, text: String) {
        val parsed = runCatching { JsonParser.parseString(text) }.getOrElse {
            throw InvalidDialogException("JSON inválido: ${it.message}")
        }
        if (!parsed.isJsonObject) throw InvalidDialogException("A tela precisa ser um objeto JSON")

        folder.mkdirs()
        val target = file(id)
        val temp = File(folder, ".${target.name}.tmp")
        temp.writeText(text, Charsets.UTF_8)
        Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private fun file(id: String): File {
        if (!ID.matches(id)) throw InvalidDialogException("Id '$id' inválido: use letras minúsculas, números, '-' e '_'")
        return File(folder, "$id.json")
    }

    private fun summary(file: File): DialogSummary {
        val obj = runCatching { JsonParser.parseString(file.readText(Charsets.UTF_8)).asJsonObject }.getOrNull()
        return DialogSummary(
            id = file.nameWithoutExtension,
            title = obj?.text("title"),
            theme = obj?.text("theme") ?: obj?.getAsJsonObject("document")?.text("theme"),
            pages = obj?.getAsJsonArray("pages")?.size() ?: 0,
            modified = file.lastModified(),
        )
    }

    private fun JsonObject.text(key: String): String? = get(key)?.takeIf { it.isJsonPrimitive }?.asString

    companion object {
        val ID = Regex("[a-z0-9_-]{1,64}")
    }
}
