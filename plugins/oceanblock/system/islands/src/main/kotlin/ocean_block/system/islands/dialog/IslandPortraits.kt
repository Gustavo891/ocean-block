package ocean_block.system.islands.dialog

import com.destroystokyo.paper.profile.PlayerProfile
import ocean_block.system.islands.island.Island
import org.bukkit.Bukkit
import java.awt.image.BufferedImage
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

object IslandPortraits {

    class Cards(val image: BufferedImage, val members: List<String>)

    private const val WAIT_SECONDS = 4L

    fun cards(island: Island): CompletableFuture<Cards> {
        val members = listOf(island.ownerName) + island.members.values.map { it.name }.sortedBy { it.lowercase() }.take(MemberCards.SLOTS - 1)
        val faces = members.map { PlayerSkins.face(it).completeOnTimeout(null, WAIT_SECONDS, TimeUnit.SECONDS).exceptionally { null } }
        return CompletableFuture.allOf(*faces.toTypedArray()).thenApply {
            val cards = members.mapIndexed { index, name ->
                MemberCards.Card(faces[index].getNow(null), owner = index == 0, online = Bukkit.getPlayerExact(name) != null)
            }
            Cards(MemberCards.render(cards), members)
        }
    }

    fun tooltip(name: String?, index: Int): String = when {
        name == null -> "Convidar jogador"
        index == 0 -> "Dono: $name"
        else -> name
    }

    fun bust(profile: PlayerProfile): CompletableFuture<BufferedImage?> =
        PlayerSkins.bust(profile).completeOnTimeout(null, WAIT_SECONDS, TimeUnit.SECONDS).exceptionally { null }
}
