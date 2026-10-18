package one_block.system.spawn.build_mode

import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object BuildMode {

    const val PERMISSION = "screen.dev"

    private val enabled = ConcurrentHashMap.newKeySet<UUID>()

    fun isEnabled(player: Player): Boolean = enabled.contains(player.uniqueId)

    fun toggle(player: Player): Boolean {
        if (enabled.remove(player.uniqueId)) return false

        enabled.add(player.uniqueId)
        return true
    }

    fun disable(uuid: UUID) {
        enabled.remove(uuid)
    }

    fun canBuild(player: Player?): Boolean =
        player != null && player.hasPermission(PERMISSION) && isEnabled(player)
}
