package ocean_block.system.battle_pass.listener

import ocean_block.libs.account.AccountResetEvent
import ocean_block.system.battle_pass.challenge.ProgressManager
import ocean_block.system.battle_pass.player.BattlePassManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class AccountResetListener : Listener {

    @EventHandler
    fun onReset(event: AccountResetEvent) {
        BattlePassManager.reset(event.player.uniqueId)
        ProgressManager.reset(event.player.uniqueId)
    }
}
