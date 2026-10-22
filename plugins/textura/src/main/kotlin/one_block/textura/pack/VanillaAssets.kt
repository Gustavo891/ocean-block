package one_block.textura.pack

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.awt.image.BufferedImage
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import java.util.zip.ZipFile
import javax.imageio.ImageIO

class VanillaAssets(private val folder: File, private val version: String, private val logger: Logger) {

    @Volatile
    private var failed = false

    fun itemTexture(name: String, download: Boolean): BufferedImage? {
        if (!NAME.matches(name)) return null
        val jar = jar(download) ?: return null
        val image = runCatching {
            ZipFile(jar).use { zip ->
                val base = read(zip, "item/$name")
                    ?: VanillaTextures.faces(name) { path -> read(zip, path)?.let(::firstFrame) }?.let(BlockIcon::render)
                    ?: VanillaTextures.find(name) { path -> read(zip, path) }
                val overlay = if (name in TINTED_POTIONS) read(zip, "item/potion_overlay") else null
                if (base != null && overlay != null) tinted(base, overlay) else base
            }
        }.getOrElse {
            logger.warning("[ui] Falha ao ler textura vanilla '$name': ${it.message}")
            null
        } ?: return null
        return firstFrame(image)
    }


    fun lang(locale: String, download: Boolean): Map<String, String> {
        if (!LOCALE.matches(locale)) return emptyMap()
        val cached = File(folder, "lang-$version-$locale.json")
        if (cached.isFile) readCachedLang(cached)?.let { return it }
        if (locale == JAR_LOCALE) {
            val jar = jar(download) ?: return emptyMap()
            return runCatching {
                ZipFile(jar).use { zip ->
                    zip.getEntry("assets/minecraft/lang/$locale.json")?.let { entry -> zip.getInputStream(entry).use { it.readBytes().decodeToString() } }
                }
            }.getOrNull()?.let(::parseLang).orEmpty()
        }
        if (!download) return emptyMap()
        return runCatching {
            synchronized(lockFor(cached)) {
                if (cached.isFile) readCachedLang(cached)?.let { return it }
                val text = fetchLang(locale)
                val parsed = parseLang(text)
                if (parsed.isEmpty()) error("idioma $locale veio vazio ou inválido")
                folder.mkdirs()
                val temp = File(folder, cached.name + ".part")
                temp.writeText(text, Charsets.UTF_8)
                Files.move(temp.toPath(), cached.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                parsed
            }
        }.getOrElse {
            logger.warning("[notificacoes] Não foi possível baixar o idioma $locale do client $version: ${it.message}")
            emptyMap()
        }
    }

    private fun readCachedLang(cached: File): Map<String, String>? {
        val parsed = runCatching { parseLang(cached.readText(Charsets.UTF_8)) }.getOrDefault(emptyMap())
        if (parsed.isNotEmpty()) return parsed
        logger.warning("[notificacoes] Cache de idioma ${cached.name} inválido; apagado para baixar de novo")
        cached.delete()
        return null
    }

    private fun fetchLang(locale: String): String {
        val manifest = json(MANIFEST)
        val entry = manifest.getAsJsonArray("versions").map { it.asJsonObject }.firstOrNull { it.get("id").asString == version }
            ?: error("versão $version não está no manifesto")
        val index = json(json(entry.get("url").asString).getAsJsonObject("assetIndex").get("url").asString)
        val hash = index.getAsJsonObject("objects").getAsJsonObject("minecraft/lang/$locale.json")?.get("hash")?.asString
            ?: error("idioma $locale não está no asset index")
        val bytes = connect("$RESOURCES/${hash.take(2)}/$hash").inputStream.use { it.readBytes() }
        val actual = MessageDigest.getInstance("SHA-1").digest(bytes).joinToString("") { "%02x".format(it) }
        if (!actual.equals(hash, ignoreCase = true)) error("sha1 diferente (esperado $hash, veio $actual)")
        return bytes.decodeToString()
    }

    private fun read(zip: ZipFile, path: String): BufferedImage? =
        zip.getEntry("assets/minecraft/textures/$path.png")?.let { entry -> zip.getInputStream(entry).use(ImageIO::read) }

    private fun tinted(base: BufferedImage, overlay: BufferedImage): BufferedImage {
        val result = BufferedImage(base.width, base.height, BufferedImage.TYPE_INT_ARGB)
        for (x in 0 until base.width) {
            for (y in 0 until base.height) {
                val liquid = if (x < overlay.width && y < overlay.height) overlay.getRGB(x, y) else 0
                val glass = base.getRGB(x, y)
                result.setRGB(x, y, if (glass ushr 24 != 0) glass else multiply(liquid, WATER_COLOR))
            }
        }
        return result
    }

    private fun multiply(argb: Int, color: Int): Int {
        val r = (argb shr 16 and 0xFF) * (color shr 16 and 0xFF) / 255
        val g = (argb shr 8 and 0xFF) * (color shr 8 and 0xFF) / 255
        val b = (argb and 0xFF) * (color and 0xFF) / 255
        return (argb and 0xFF000000.toInt()) or (r shl 16) or (g shl 8) or b
    }

    private fun firstFrame(image: BufferedImage): BufferedImage =
        if (image.height > image.width && image.height % image.width == 0) image.getSubimage(0, 0, image.width, image.width) else image

    private fun jar(download: Boolean): File? {
        val file = File(folder, "client-$version.jar")
        if (file.isFile) return file
        if (!download || failed) return null
        synchronized(lockFor(file)) {
            if (file.isFile) return file
            return runCatching { fetch(file) }.getOrElse {
                failed = true
                logger.warning("[ui] Não foi possível baixar o client $version da Mojang (ícones item: ficam vazios): ${it.message}")
                null
            }
        }
    }

    private fun fetch(target: File): File {
        logger.info("[ui] Baixando texturas vanilla do client $version...")
        val manifest = json(MANIFEST)
        val entry = manifest.getAsJsonArray("versions").map { it.asJsonObject }.firstOrNull { it.get("id").asString == version }
            ?: error("versão $version não está no manifesto")
        val client = json(entry.get("url").asString).getAsJsonObject("downloads").getAsJsonObject("client")
        val sha1 = client.get("sha1").asString

        folder.mkdirs()
        val temp = File(folder, target.name + ".part")
        connect(client.get("url").asString).inputStream.use { input -> temp.outputStream().use { input.copyTo(it) } }
        val actual = sha1(temp)
        if (!actual.equals(sha1, ignoreCase = true)) {
            temp.delete()
            error("sha1 diferente (esperado $sha1, veio $actual)")
        }
        if (!temp.renameTo(target)) error("não foi possível mover ${temp.name}")
        logger.info("[ui] Client $version salvo em ${target.path}")
        return target
    }

    private fun json(url: String): JsonObject =
        connect(url).inputStream.bufferedReader().use { JsonParser.parseReader(it).asJsonObject }

    private fun connect(url: String): HttpURLConnection =
        (URI(url).toURL().openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 120_000
            if (responseCode !in 200..299) error("HTTP $responseCode em $url")
        }

    private fun sha1(file: File): String {
        val digest = MessageDigest.getInstance("SHA-1")
        file.inputStream().use { input ->
            val buffer = ByteArray(65_536)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        private val LOCKS = ConcurrentHashMap<String, Any>()

        fun lockFor(file: File): Any = LOCKS.computeIfAbsent(file.absoluteFile.normalize().path) { Any() }

        const val MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
        const val RESOURCES = "https://resources.download.minecraft.net"
        const val JAR_LOCALE = "en_us"
        val LOCALE = Regex("[a-z]{2,3}_[a-z]{2,3}")
        val NAME = Regex("[a-z0-9_]+")
        val TINTED_POTIONS = setOf("potion", "splash_potion", "lingering_potion")
        const val WATER_COLOR = 0x385DC6
    }
}

fun parseLang(json: String): Map<String, String> =
    runCatching {
        JsonParser.parseString(json).asJsonObject.entrySet()
            .filter { it.value.isJsonPrimitive }
            .associate { it.key to it.value.asString }
    }.getOrDefault(emptyMap())
