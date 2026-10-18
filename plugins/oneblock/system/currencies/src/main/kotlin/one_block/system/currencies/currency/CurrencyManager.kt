package one_block.system.currencies.currency

import co.aikar.commands.PaperCommandManager
import one_block.libs.instance.Instance
import one_block.system.currencies.command.CurrencyCommand
import one_block.system.currencies.expansion.WalletExpansion
import one_block.system.currencies.wallet.WalletManager
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object CurrencyManager {

    private val currencies = mutableMapOf<String, WalletManager>()

    fun onInit() {
        val executor = Executors.newSingleThreadScheduledExecutor()
        executor.scheduleAtFixedRate({
            currencies.values.forEach { it.saveDirty() }
        }, 30, 30, TimeUnit.SECONDS)
    }

    fun onDisable() {
        currencies.values.forEach { it.saveDirty() }
    }

    fun find(id: String) = currencies[id.lowercase()]

    fun load(commandManager: PaperCommandManager) {
        val folder = File(Instance.dataFolder, "currencies")
        if (!folder.exists()) {
            folder.mkdirs()
        }

        folder.listFiles { file ->
            file.isFile && file.name.endsWith(".yml")
        }?.forEach { file ->
            val config = YamlConfiguration.loadConfiguration(file)

            val id = config.getString("id") ?: return@forEach
            val prettyName = config.getString("pretty-name") ?: id
            val initialValue = config.getDouble("initial-value")

            val currency = Currency(id, prettyName, initialValue)
            val walletManager = WalletManager(currency)

            commandManager.registerCommand(CurrencyCommand(walletManager))
            WalletExpansion(walletManager).register()

            currencies[id] = walletManager
        }

        Instance.logger.info("Loaded ${currencies.size} currencies.")
    }

    fun toList() = currencies.values.toList()
}
