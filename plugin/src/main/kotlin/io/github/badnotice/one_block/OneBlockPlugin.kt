package io.github.badnotice.one_block

import co.aikar.commands.PaperCommandManager
import io.github.badnotice.one_block.command.OneBlockCommand
import me.devnatan.inventoryframework.ViewFrame
import one_block.libs.instance.PluginInstance
import one_block.libs.instance.ScheduledExecutorInstance
import one_block.system.arena.ArenaSystem
import one_block.system.auction_house.AuctionHouseSystem
import one_block.system.auction_house.command.AuctionCommand
import one_block.system.chats.ChatsSystem
import one_block.system.coin_flip.CoinFlipSystem
import one_block.system.coin_flip.coin_flip.CoinFlipManager
import one_block.system.coin_flip.command.CoinFlipCommand
import one_block.system.currencies.CurrenciesSystem
import one_block.system.islands.IslandsSystem
import one_block.system.islands.command.IslandCommand
import one_block.system.islands.listener.IslandListener
import one_block.system.islands.listener.PlayerListener
import one_block.system.items.SpecialItemSystem
import one_block.system.items.commands.ItemCommand
import one_block.system.minions.MinionsSystem
import one_block.system.playtime.PlaytimeSystem
import one_block.system.sell.SellSystem
import one_block.system.sell.command.SellCommand
import one_block.system.shop.ShopSystem
import one_block.system.shop.command.ShopCommand
import one_block.system.starter_kit.StarterKitSystem
import one_block.system.tops.TopsSystem
import one_block.system.tops.command.TopsCommand
import one_block.system.worlds.WorldsSystem
import one_block.system.worlds.command.WorldCommand
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.Executors

class OneBlockPlugin : JavaPlugin() {

    private val paperCommandManager by lazy { PaperCommandManager(this) }
    private val viewFrame by lazy { ViewFrame.create(this) }

    override fun onEnable() {
        PluginInstance = this
        ScheduledExecutorInstance = Executors.newSingleThreadScheduledExecutor()

        setupSystems()
        setupCommands()
        setupListeners()

        viewFrame.register()
    }

    override fun onDisable() {
        CoinFlipManager.onDisable()
    }

    private fun setupSystems() {
        ArenaSystem.init(this, viewFrame)
        AuctionHouseSystem.init(this, viewFrame)
        ChatsSystem.init(this)
        CoinFlipSystem.init(this, viewFrame)
        CurrenciesSystem.init(this, paperCommandManager)
        IslandsSystem.init(this, viewFrame)
        MinionsSystem.init(viewFrame, paperCommandManager)
        PlaytimeSystem.init(this)
        SellSystem.init(this, viewFrame)
        ShopSystem.init(this, viewFrame)
        StarterKitSystem.init(this)
        TopsSystem.init(this, viewFrame)
        WorldsSystem.init(this)
        SpecialItemSystem.init(this)
    }

    private fun setupCommands() {
        with(paperCommandManager) {
            registerCommand(AuctionCommand(viewFrame))
            registerCommand(CoinFlipCommand(viewFrame))
            registerCommand(OneBlockCommand())
            registerCommand(IslandCommand(viewFrame))
            registerCommand(SellCommand(viewFrame))
            registerCommand(ShopCommand(viewFrame))
            registerCommand(TopsCommand(viewFrame))
            registerCommand(WorldCommand())
            registerCommand(ItemCommand())

            ArenaSystem.setupCommands(this)
        }
    }

    private fun setupListeners() = with(server.pluginManager) {
        registerEvents(PlayerListener(), this@OneBlockPlugin)
        registerEvents(IslandListener(), this@OneBlockPlugin)
        registerEvents(one_block.system.sell.listener.PlayerListener(), this@OneBlockPlugin)
    }
}