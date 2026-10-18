package ocean_block.system.shop

import me.devnatan.inventoryframework.ViewFrame
import ocean_block.system.shop.shop.ShopManager
import ocean_block.system.shop.view.ProductView
import ocean_block.system.shop.view.PurchaseView
import ocean_block.system.shop.view.ShopView
import org.bukkit.plugin.java.JavaPlugin

object ShopSystem {

    fun init(plugin: JavaPlugin, viewFrame: ViewFrame) {
        ShopManager.load()
        viewFrame.with(ShopView(), ProductView(), PurchaseView())
    }
}
