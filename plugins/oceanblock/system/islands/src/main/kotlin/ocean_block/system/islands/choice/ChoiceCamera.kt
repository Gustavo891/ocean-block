package ocean_block.system.islands.choice

import ocean_block.libs.instance.Instance
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType

object ChoiceCamera {

    private const val WORLD = "world"
    private const val X = 0.5
    private const val Y = 80.0
    private const val Z = 0.5
    private const val PITCH = 10f

    private val MARK: NamespacedKey by lazy { NamespacedKey(Instance, "escolhendo_ilha") }

    fun location(): Location? = Bukkit.getWorld(WORLD)?.let { Location(it, X, Y, Z, 0f, PITCH) }

    fun enter(player: Player) {
        player.persistentDataContainer.set(MARK, PersistentDataType.BYTE, 1)
        player.gameMode = GameMode.SPECTATOR
        location()?.let { player.teleport(it) }
    }

    fun leave(player: Player) {
        if (!isMarked(player)) return
        player.persistentDataContainer.remove(MARK)
        player.gameMode = GameMode.SURVIVAL
    }

    fun isMarked(player: Player): Boolean = player.persistentDataContainer.has(MARK, PersistentDataType.BYTE)
}
