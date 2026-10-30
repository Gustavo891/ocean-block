package ocean_block.system.collections.listener

import ocean_block.libs.account.AccountResetEvent
import ocean_block.system.collections.collection.CollectionManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class AccountResetListener : Listener {

    @EventHandler
    fun onReset(event: AccountResetEvent) {
        CollectionManager.reset(event.player.uniqueId, event.player.name)
    }
}
