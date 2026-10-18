package io.github.badnotice.one_block

import co.aikar.commands.PaperCommandManager
import one_block.system.custom_items.content.multitool.MultiToolCommand
import io.github.badnotice.one_block.command.OneBlockCommand
import me.devnatan.inventoryframework.ViewFrame
import one_block.libs.instance.PluginInstance
import one_block.libs.instance.ScheduledExecutorInstance
import one_block.system.arena.ArenaSystem
import one_block.system.auction_house.AuctionHouseSystem
import one_block.system.auction_house.command.AuctionCommand
import one_block.system.coin_flip.CoinFlipSystem
import one_block.system.coin_flip.coin_flip.CoinFlipManager
import one_block.system.coin_flip.command.CoinFlipCommand
import one_block.system.combatlog.CombatLogSystem
import one_block.system.collections.CollectionsSystem
import one_block.system.custom_items.CustomItemSystem
import one_block.system.custom_items.block.PlacedBlockManager
import one_block.system.custom_items.item.CustomItemCommand
import one_block.system.craft.CraftSystem
import one_block.system.currencies.CurrenciesSystem
import one_block.system.currencies.currency.CurrencyManager
import one_block.system.enchants.EnchantsSystem
import one_block.system.skills.SkillsSystem
import one_block.system.fly_timer.FlyTimerSystem
import one_block.system.fly_timer.fly_player.FlyPlayerManager
import one_block.system.islands.IslandsSystem
import one_block.system.islands.command.IslandCommand
import one_block.system.islands.listener.IslandListener
import one_block.system.islands.listener.IslandSettingListener
import one_block.system.islands.listener.PlayerListener
import one_block.system.special_items.SpecialItemSystem
import one_block.system.special_items.sell.SellBeaconManager
import one_block.system.special_items.commands.ItemCommand
import one_block.system.battle_pass.BattlePassSystem
import one_block.system.minions.MinionsSystem
import one_block.system.minions.placed_minion.PlacedMinionManager
import one_block.system.mobdrops.MobDropsSystem
import one_block.system.playtime.PlaytimeSystem
import one_block.system.playtime.player_playtime.PlayerPlaytimeManager
import one_block.system.repair.RepairsSystem
import one_block.system.scoreboard.ScoreboardSystem
import one_block.system.sell.SellSystem
import one_block.system.sell.command.SellCommand
import one_block.system.sell.command.SellItemsCommand
import one_block.system.shop.ShopSystem
import one_block.system.shop.command.ShopCommand
import one_block.system.spawn.SpawnSystem
import one_block.system.spawners.SpawnersSystem
import one_block.system.starter_kit.StarterKitSystem
import one_block.system.tops.TopsSystem
import one_block.system.tops.command.TopsCommand
import one_block.system.worlds.WorldsSystem
import one_block.system.worlds.command.WorldCommand
import one_block.system.xpbottle.XpBottleSystem
import one_block.system.trader.TraderSystem
import one_block.system.trader.command.TraderCommand
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
        PlacedMinionManager.onDisable()
        PlacedBlockManager.onDisable()
        SpawnersSystem.onDisable()
        SkillsSystem.onDisable()
        CollectionsSystem.onDisable()
        SpawnSystem.onDisable()
        ScoreboardSystem.onDisable()
        IslandsSystem.onDisable()
        CurrencyManager.onDisable()
        PlayerPlaytimeManager.onDisable()
        FlyPlayerManager.onDisable()
        SellBeaconManager.onDisable()
    }

    private fun setupSystems() {
        val systems = listOf(
            { CustomItemSystem.init(this, viewFrame, paperCommandManager) },
            { ArenaSystem.init(this, viewFrame) },
            { AuctionHouseSystem.init(this, viewFrame) },
            { SpawnersSystem.init(this, viewFrame, paperCommandManager) },
            { CombatLogSystem.init(this, viewFrame)},
            { CoinFlipSystem.init(this, viewFrame) },
            { CraftSystem.init(viewFrame, paperCommandManager) },
            { CurrenciesSystem.init(this, paperCommandManager) },
            { SkillsSystem.init(this, viewFrame, paperCommandManager) },
            { CollectionsSystem.init(this, viewFrame, paperCommandManager) },
            { EnchantsSystem.init(this, paperCommandManager) },
            { FlyTimerSystem.init(paperCommandManager) },
            { IslandsSystem.init(this, viewFrame) },
            { BattlePassSystem.init(viewFrame, paperCommandManager) },
            { MinionsSystem.init(viewFrame, paperCommandManager) },
            { MobDropsSystem.init(viewFrame, paperCommandManager) },
            { PlaytimeSystem.init(this) },
            { RepairsSystem.init(this, paperCommandManager)},
            { ScoreboardSystem.init(this, paperCommandManager) },
            { SellSystem.init(this, viewFrame) },
            { ShopSystem.init(this, viewFrame) },
            { SpawnSystem.init(this, paperCommandManager) },
            { SpecialItemSystem.init(this) },
            { StarterKitSystem.init(this) },
            { TopsSystem.init(this, viewFrame) },
            { WorldsSystem.init(this) },
            { XpBottleSystem.init(this, paperCommandManager) },
            { TraderSystem.init(this, viewFrame) },
        )

        systems.forEach { it.invoke() }
    }

    private fun setupCommands() {
        with(paperCommandManager) {
            commandCompletions.registerCompletion("phases") {
                one_block.system.islands.phase.PhaseManager.phases().map { phase -> phase.id }
            }

            registerCommand(AuctionCommand(viewFrame))
            registerCommand(CoinFlipCommand(viewFrame))
            registerCommand(OneBlockCommand())
            registerCommand(TraderCommand())
            registerCommand(IslandCommand(viewFrame))
            registerCommand(SellCommand(viewFrame))
            registerCommand(SellItemsCommand(viewFrame))
            registerCommand(ShopCommand(viewFrame))
            registerCommand(TopsCommand(viewFrame))
            registerCommand(WorldCommand())
            registerCommand(ItemCommand())
            registerCommand(MultiToolCommand())
            registerCommand(CustomItemCommand())

            ArenaSystem.setupCommands(this)
        }
    }

    private fun setupListeners() = with(server.pluginManager) {
        registerEvents(PlayerListener(), this@OneBlockPlugin)
        registerEvents(IslandListener(), this@OneBlockPlugin)
        registerEvents(IslandSettingListener(), this@OneBlockPlugin)
        registerEvents(one_block.system.sell.listener.PlayerListener(), this@OneBlockPlugin)
        registerEvents(io.github.badnotice.one_block.listener.MobControlListener(), this@OneBlockPlugin)
    }
}
