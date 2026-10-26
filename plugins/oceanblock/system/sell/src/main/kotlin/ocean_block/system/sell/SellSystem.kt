package ocean_block.system.sell

import me.devnatan.inventoryframework.ViewFrame
import ocean_block.system.sell.manager.SellManager
import ocean_block.system.sell.notify.SaleNotifier
import one_block.textura.api.NotificationApi
import ocean_block.system.sell.view.SellItemsView
import ocean_block.system.sell.view.SellView
import org.bukkit.plugin.java.JavaPlugin

object SellSystem {

    private const val SAMPLE_TOTAL = 1500.0
    private const val SAMPLE_AMOUNT = 32

    lateinit var plugin: JavaPlugin

    fun init(plugin: JavaPlugin, viewFrame: ViewFrame) {
        this.plugin = plugin
        SellManager.load()
        NotificationApi.registerSample("venda") { SaleNotifier.notify(it, SAMPLE_TOTAL, SAMPLE_AMOUNT) }
        viewFrame.with(SellView())
        viewFrame.with(SellItemsView())
    }
}
