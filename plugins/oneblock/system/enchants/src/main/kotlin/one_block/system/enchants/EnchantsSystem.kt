package one_block.system.enchants

import co.aikar.commands.PaperCommandManager
import one_block.system.enchants.command.EnchantCommand
import one_block.system.enchants.content.AutoSmeltEnchant
import one_block.system.enchants.content.TelekinesisEnchant
import one_block.system.enchants.content.VampirismEnchant
import one_block.system.enchants.enchant.EnchantRegistry
import one_block.system.enchants.listener.EnchantBookListener
import one_block.system.enchants.listener.EnchantListener
import org.bukkit.plugin.java.JavaPlugin

object EnchantsSystem {

    fun init(plugin: JavaPlugin, commandManager: PaperCommandManager) {
        registerBuiltInEnchants()

        commandManager.registerCommand(EnchantCommand())

        setupListeners(plugin)
    }

    private fun registerBuiltInEnchants() {
        EnchantRegistry.register(TelekinesisEnchant())
        EnchantRegistry.register(AutoSmeltEnchant())
        EnchantRegistry.register(VampirismEnchant())
    }

    private fun setupListeners(plugin: JavaPlugin) = with(plugin.server.pluginManager) {
        registerEvents(EnchantListener(), plugin)
        registerEvents(EnchantBookListener(), plugin)
    }
}
