package ocean_block.system.currencies

import co.aikar.commands.PaperCommandManager
import ocean_block.system.currencies.currency.CurrencyManager
import ocean_block.system.currencies.listener.AccountResetListener
import ocean_block.system.currencies.listener.PlayerListener
import org.bukkit.plugin.java.JavaPlugin

object CurrenciesSystem {

    fun init(plugin: JavaPlugin, commandManager: PaperCommandManager) {
        CurrencyManager.onInit()
        CurrencyManager.load(commandManager)

        setupListeners(plugin)
    }

    private fun setupListeners(plugin: JavaPlugin) = with(plugin.server.pluginManager) {
        registerEvents(PlayerListener(), plugin)
        registerEvents(AccountResetListener(), plugin)
    }
}
