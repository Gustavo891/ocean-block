package one_block.textura.listener

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerResourcePackStatusEvent
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PackStatusTracker(private val packId: UUID, private val onQuit: (UUID) -> Unit) : Listener {

    private val loaded = ConcurrentHashMap.newKeySet<UUID>()

    fun loaded(player: Player): Boolean = player.uniqueId in loaded

    fun reset() {
        loaded.clear()
    }

    @EventHandler
    fun onStatus(event: PlayerResourcePackStatusEvent) {
        if (event.id != packId) return
        val id = event.player.uniqueId
        when (event.status) {
            Status.SUCCESSFULLY_LOADED -> loaded += id
            Status.DECLINED, Status.FAILED_DOWNLOAD, Status.INVALID_URL, Status.FAILED_RELOAD, Status.DISCARDED -> loaded -= id
            else -> Unit
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        loaded -= event.player.uniqueId
        onQuit(event.player.uniqueId)
    }
}
