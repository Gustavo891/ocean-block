package io.github.badnotice.ocean_block

import co.aikar.commands.PaperCommandManager
import ocean_block.system.custom_items.content.multitool.MultiToolCommand
import io.github.badnotice.ocean_block.command.OceanBlockCommand
import me.devnatan.inventoryframework.ViewFrame
import ocean_block.libs.instance.PluginInstance
import ocean_block.libs.instance.ScheduledExecutorInstance
import ocean_block.system.auction_house.AuctionHouseSystem
import ocean_block.system.auction_house.command.AuctionCommand
import ocean_block.system.coin_flip.CoinFlipSystem
import ocean_block.system.coin_flip.coin_flip.CoinFlipManager
import ocean_block.system.coin_flip.command.CoinFlipCommand
import ocean_block.system.collections.CollectionsSystem
import ocean_block.system.custom_items.CustomItemSystem
import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.custom_items.item.CustomItemCommand
import ocean_block.system.craft.CraftSystem
import ocean_block.system.drift.DriftSystem
import ocean_block.system.machines.MachinesSystem
import ocean_block.system.preferences.PreferencesSystem
import ocean_block.system.currencies.CurrenciesSystem
import ocean_block.system.currencies.currency.CurrencyManager
import ocean_block.system.enchants.EnchantsSystem
import ocean_block.system.islands.IslandsSystem
import ocean_block.system.islands.command.IslandCommand
import ocean_block.system.islands.listener.IslandListener
import ocean_block.system.islands.listener.IslandSettingListener
import ocean_block.system.islands.listener.PlayerListener
import ocean_block.system.battle_pass.BattlePassSystem
import ocean_block.system.playtime.PlaytimeSystem
import ocean_block.system.playtime.player_playtime.PlayerPlaytimeManager
import ocean_block.system.level.LevelSystem
import ocean_block.system.missions.MissionsSystem
import ocean_block.system.scoreboard.ScoreboardSystem
import ocean_block.system.sell.SellSystem
import ocean_block.system.sell.command.SellCommand
import ocean_block.system.sell.command.SellItemsCommand
import ocean_block.system.shop.ShopSystem
import ocean_block.system.shop.command.ShopCommand
import ocean_block.system.spawn.SpawnSystem
import ocean_block.system.starter_kit.StarterKitSystem
import ocean_block.system.tops.TopsSystem
import ocean_block.system.tops.command.TopsCommand
import ocean_block.system.worlds.WorldsSystem
import ocean_block.system.worlds.command.WorldCommand
import ocean_block.system.trader.TraderSystem
import ocean_block.system.trader.command.TraderCommand
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.Executors

class OceanBlockPlugin : JavaPlugin() {

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
        DriftSystem.onDisable()
        MachinesSystem.onDisable()
        CoinFlipManager.onDisable()
        PlacedBlockManager.onDisable()
        CollectionsSystem.onDisable()
        SpawnSystem.onDisable()
        LevelSystem.onDisable()
        MissionsSystem.onDisable()
        ScoreboardSystem.onDisable()
        IslandsSystem.onDisable()
        CurrencyManager.onDisable()
        PlayerPlaytimeManager.onDisable()
    }

    private fun setupSystems() {
        val systems = listOf(
            { CustomItemSystem.init(this, viewFrame, paperCommandManager) },
            { AuctionHouseSystem.init(this, viewFrame) },
            { CoinFlipSystem.init(this, viewFrame) },
            { CraftSystem.init(this, viewFrame, paperCommandManager) },
            { CurrenciesSystem.init(this, paperCommandManager) },
            { CollectionsSystem.init(this, viewFrame, paperCommandManager) },
            { EnchantsSystem.init(this, paperCommandManager) },
            { IslandsSystem.init(this, viewFrame) },
            { DriftSystem.init(this, viewFrame) },
            { PreferencesSystem.init(viewFrame, paperCommandManager) },
            { MachinesSystem.init(this, viewFrame) },
            { BattlePassSystem.init(viewFrame, paperCommandManager) },
            { PlaytimeSystem.init(this) },
            { ScoreboardSystem.init(this, paperCommandManager) },
            { SellSystem.init(this, viewFrame) },
            { LevelSystem.init(this, viewFrame, paperCommandManager) },
            { MissionsSystem.init(this, paperCommandManager) },
            { ShopSystem.init(this, viewFrame) },
            { SpawnSystem.init(this, paperCommandManager) },
            { StarterKitSystem.init(this) },
            { TopsSystem.init(this, viewFrame) },
            { WorldsSystem.init(this) },
            { TraderSystem.init(this, viewFrame) },
        )

        systems.forEach { it.invoke() }
    }

    private fun setupCommands() {
        with(paperCommandManager) {
            registerCommand(AuctionCommand(viewFrame))
            registerCommand(CoinFlipCommand(viewFrame))
            registerCommand(OceanBlockCommand())
            registerCommand(TraderCommand())
            registerCommand(IslandCommand(viewFrame))
            registerCommand(SellCommand(viewFrame))
            registerCommand(SellItemsCommand(viewFrame))
            registerCommand(ShopCommand(viewFrame))
            registerCommand(TopsCommand(viewFrame))
            registerCommand(WorldCommand())
            registerCommand(MultiToolCommand())
            registerCommand(CustomItemCommand())
        }
    }

    private fun setupListeners() = with(server.pluginManager) {
        registerEvents(PlayerListener(), this@OceanBlockPlugin)
        registerEvents(IslandListener(), this@OceanBlockPlugin)
        registerEvents(IslandSettingListener(), this@OceanBlockPlugin)
        registerEvents(ocean_block.system.sell.listener.PlayerListener(), this@OceanBlockPlugin)
        registerEvents(io.github.badnotice.ocean_block.listener.MobControlListener(), this@OceanBlockPlugin)
    }
}
