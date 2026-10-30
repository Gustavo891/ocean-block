package ocean_block.system.level.listener

import ocean_block.libs.account.AccountResetEvent
import ocean_block.system.level.PlayerLevels
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class AccountResetListener : Listener {

    @EventHandler
    fun onReset(event: AccountResetEvent) {
        PlayerLevels.setXp(event.player, 0)
    }
}
