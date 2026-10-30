package ocean_block.system.currencies.listener

import ocean_block.libs.account.AccountResetEvent
import ocean_block.system.currencies.currency.CurrencyManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class AccountResetListener : Listener {

    @EventHandler
    fun onReset(event: AccountResetEvent) {
        CurrencyManager.all().forEach { manager ->
            manager.find(event.player.uniqueId)?.let { manager.setBalance(it, manager.currency.initialValue) }
        }
    }
}
