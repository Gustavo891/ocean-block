package ocean_block.libs.scoreboard

import fr.mrmicky.fastboard.adventure.FastBoard
import net.kyori.adventure.text.Component
import ocean_block.libs.instance.Instance
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

internal object SidebarService {

    private val boards = ConcurrentHashMap<UUID, FastBoard>()
    private val listening = AtomicBoolean()

    fun has(player: Player): Boolean = boards.containsKey(player.uniqueId)

    fun render(player: Player, title: Component, lines: List<SidebarLine>, hideNumbers: Boolean) {
        listen()

        val board = boardOf(player)

        board.updateTitle(title)
        board.updateLines(lines.map { it.text })

        val scores: List<Component?> = lines.map { line ->
            if (hideNumbers) null else Component.text(line.score)
        }

        @Suppress("UNCHECKED_CAST")
        board.updateScores(scores as Collection<Component>)
    }

    fun hide(player: Player) {
        val board = boards.remove(player.uniqueId) ?: return
        if (!board.isDeleted) board.delete()
    }

    fun forget(uuid: UUID) {
        boards.remove(uuid)
    }

    private fun boardOf(player: Player): FastBoard {
        val existing = boards[player.uniqueId]
        if (existing != null && !existing.isDeleted) return existing

        return FastBoard(player).also { boards[player.uniqueId] = it }
    }

    private fun listen() {
        if (!listening.compareAndSet(false, true)) return

        Bukkit.getPluginManager().registerEvents(
            object : Listener {
                @EventHandler
                fun onQuit(event: PlayerQuitEvent) = forget(event.player.uniqueId)
            },
            Instance,
        )
    }
}
