package ocean_block.system.auction_house

import me.devnatan.inventoryframework.ViewFrame
import ocean_block.system.auction_house.auction.AuctionManager
import ocean_block.system.auction_house.view.AuctionView
import org.bukkit.plugin.java.JavaPlugin

object AuctionHouseSystem {

    fun init(plugin: JavaPlugin, viewFrame: ViewFrame) {
        AuctionManager.onInit()
        viewFrame.with(AuctionView())
    }
}
