package ocean_block.system.islands.account

import ocean_block.system.islands.ext.runNextTask
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class AccountResetListener : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        runNextTask { if (player.isOnline) AccountReset.consume(player) }
    }
}
