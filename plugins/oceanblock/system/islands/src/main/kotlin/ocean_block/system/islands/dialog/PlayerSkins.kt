package ocean_block.system.islands.dialog

import com.destroystokyo.paper.profile.PlayerProfile
import ocean_block.libs.instance.Instance
import org.bukkit.Bukkit
import org.bukkit.profile.PlayerTextures
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

object PlayerSkins {

    class Skin(val image: BufferedImage, val slim: Boolean)

    const val STAGE = 0xFF1F2124.toInt()

    private const val MAX_CACHED = 128
    private val TIMEOUT: Duration = Duration.ofSeconds(4)

    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).followRedirects(HttpClient.Redirect.NORMAL).build()
    private val skins = ConcurrentHashMap<String, Skin>()
    private val busts = ConcurrentHashMap<String, BufferedImage>()

    private val fallbackSkin = Skin(DefaultSkin.image(), slim = false)
    private val fallbackBust: BufferedImage by lazy { PlayerFigure.bust(fallbackSkin.image, fallbackSkin.slim, STAGE) }

    fun bust(profile: PlayerProfile): CompletableFuture<BufferedImage> {
        val key = key(profile) ?: return CompletableFuture.completedFuture(fallbackBust)
        busts[key]?.let { return CompletableFuture.completedFuture(it) }
        return skin(profile).thenApply { skin ->
            if (skin === fallbackSkin) fallbackBust else PlayerFigure.bust(skin.image, skin.slim, STAGE).also { remember(busts, key, it) }
        }
    }

    fun face(name: String): CompletableFuture<BufferedImage> {
        val online = Bukkit.getPlayerExact(name)?.playerProfile
        val profile = online ?: Bukkit.createProfile(null, name)
        val complete = if (profile.hasTextures()) CompletableFuture.completedFuture(profile) else profile.update()
        return complete
            .thenCompose { skin(it) }
            .thenApply { MemberSlots.face(it.image) }
            .exceptionally { MemberSlots.face(fallbackSkin.image) }
    }

    private fun skin(profile: PlayerProfile): CompletableFuture<Skin> {
        val textures = profile.textures
        val url = textures.skin ?: return CompletableFuture.completedFuture(fallbackSkin)
        val slim = textures.skinModel == PlayerTextures.SkinModel.SLIM
        val key = "$url#$slim"
        skins[key]?.let { return CompletableFuture.completedFuture(it) }

        val request = HttpRequest.newBuilder(URI.create(url.toString().replace("http://", "https://"))).timeout(TIMEOUT).GET().build()
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
            .thenApply { response ->
                val image = response.takeIf { it.statusCode() == 200 }?.body()?.let { ImageIO.read(ByteArrayInputStream(it)) }
                    ?: error("skin ${response.statusCode()}")
                Skin(image, slim).also { remember(skins, key, it) }
            }
            .exceptionally { error ->
                Instance.logger.warning("[ilha] skin de ${profile.name} indisponível: ${error.message}")
                fallbackSkin
            }
    }

    private fun key(profile: PlayerProfile): String? {
        val textures = profile.textures
        val url = textures.skin ?: return null
        return "$url#${textures.skinModel == PlayerTextures.SkinModel.SLIM}"
    }

    private fun <T> remember(cache: ConcurrentHashMap<String, T>, key: String, value: T) {
        if (cache.size >= MAX_CACHED) cache.clear()
        cache[key] = value
    }
}
