package ocean_block.system.playtime.listener

import ocean_block.libs.account.AccountResetEvent
import ocean_block.system.playtime.player_playtime.PlayerPlaytimeManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class AccountResetListener : Listener {

    @EventHandler
    fun onReset(event: AccountResetEvent) {
        PlayerPlaytimeManager.get(event.player.uniqueId)?.reset()
    }
}
