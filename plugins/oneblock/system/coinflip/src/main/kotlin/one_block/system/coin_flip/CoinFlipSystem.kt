package one_block.system.coin_flip

import me.devnatan.inventoryframework.ViewFrame
import one_block.system.coin_flip.coin_flip.CoinFlipManager
import one_block.system.coin_flip.view.CoinFlipView
import org.bukkit.plugin.java.JavaPlugin

object CoinFlipSystem {

    fun init(plugin: JavaPlugin, viewFrame: ViewFrame) {
        CoinFlipManager.onInit()

        setupListeners(plugin)
        viewFrame.with(CoinFlipView())
    }

    private fun setupListeners(plugin: JavaPlugin) = with(plugin.server.pluginManager) {
    }
}
