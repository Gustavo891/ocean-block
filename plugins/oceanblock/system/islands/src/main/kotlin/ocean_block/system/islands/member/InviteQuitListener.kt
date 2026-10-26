package ocean_block.system.islands.member

import ocean_block.system.islands.dialog.IslandDialog
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class InviteQuitListener : Listener {

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        IslandMembership.forget(event.player.uniqueId)
        IslandDialog.forget(event.player.uniqueId)
    }
}
