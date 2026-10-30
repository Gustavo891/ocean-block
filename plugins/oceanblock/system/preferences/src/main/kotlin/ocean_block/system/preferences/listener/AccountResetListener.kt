package ocean_block.system.preferences.listener

import ocean_block.libs.account.AccountResetEvent
import ocean_block.system.preferences.Preferences
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class AccountResetListener : Listener {

    @EventHandler
    fun onReset(event: AccountResetEvent) {
        Preferences.reset(event.player)
    }
}
