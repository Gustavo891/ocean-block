package ocean_block.system.playtime_rewards.listener

import ocean_block.libs.account.AccountResetEvent
import ocean_block.system.playtime_rewards.PlaytimeRewards
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class AccountResetListener : Listener {

    @EventHandler
    fun onReset(event: AccountResetEvent) {
        PlaytimeRewards.reset(event.player)
    }
}
