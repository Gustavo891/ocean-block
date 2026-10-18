package ocean_block.system.scoreboard.listener

import ocean_block.system.scoreboard.board.BoardManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class ScoreboardListener : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onJoin(event: PlayerJoinEvent) {
        BoardManager.render(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onChangeWorld(event: PlayerChangedWorldEvent) {
        BoardManager.render(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onQuit(event: PlayerQuitEvent) {
        BoardManager.forget(event.player)
    }
}
