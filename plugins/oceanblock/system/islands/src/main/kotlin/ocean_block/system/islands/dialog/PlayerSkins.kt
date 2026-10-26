package ocean_block.system.islands.dialog

import ocean_block.libs.instance.Instance
import org.bukkit.entity.Player
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

    private const val MAX_CACHED = 128
    private val TIMEOUT: Duration = Duration.ofSeconds(4)

    private const val FRAMES = 12

    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).followRedirects(HttpClient.Redirect.NORMAL).build()

    private val figures = ConcurrentHashMap<String, List<BufferedImage>>()

    fun frames(player: Player): CompletableFuture<List<BufferedImage>> {
        val textures = player.playerProfile.textures
        val url = textures.skin ?: return CompletableFuture.completedFuture(fallback())
        val slim = textures.skinModel == PlayerTextures.SkinModel.SLIM
        val key = "$url#$slim"
        figures[key]?.let { return CompletableFuture.completedFuture(it) }

        val request = HttpRequest.newBuilder(URI.create(url.toString().replace("http://", "https://"))).timeout(TIMEOUT).GET().build()
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
            .thenApply { response ->
                val skin = response.takeIf { it.statusCode() == 200 }?.body()?.let { ImageIO.read(ByteArrayInputStream(it)) }
                    ?: error("skin ${response.statusCode()}")
                PlayerFigure.frames(skin, slim, FRAMES).also { remember(key, it) }
            }
            .exceptionally { error ->
                Instance.logger.warning("[ilha] skin de ${player.name} indisponível: ${error.message}")
                fallback()
            }
    }

    private fun remember(key: String, figure: List<BufferedImage>) {
        if (figures.size >= MAX_CACHED) figures.clear()
        figures[key] = figure
    }

    private val fallbackFrames: List<BufferedImage> by lazy { PlayerFigure.frames(DefaultSkin.image(), slim = false, count = FRAMES) }

    private fun fallback(): List<BufferedImage> = fallbackFrames
}
