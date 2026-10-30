package ocean_block.system.auction_house.listener

import ocean_block.libs.account.AccountResetEvent
import ocean_block.system.auction_house.auction.AuctionManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class AccountResetListener : Listener {

    @EventHandler
    fun onReset(event: AccountResetEvent) {
        AuctionManager.auctions.values
            .filter { it.sellerUuid == event.player.uniqueId }
            .forEach { AuctionManager.unregister(it.uuid) }
    }
}
