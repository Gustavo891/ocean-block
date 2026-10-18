package one_block.system.custom_items

import co.aikar.commands.PaperCommandManager
import me.devnatan.inventoryframework.ViewFrame
import one_block.system.craft.Crafts
import one_block.system.custom_items.block.CustomBlockCommand
import one_block.system.custom_items.block.CustomBlockListener
import one_block.system.custom_items.block.CustomBlockRegistry
import one_block.system.custom_items.block.PlacedBlockManager
import one_block.system.custom_items.content.magnet.MagnetItem
import one_block.system.custom_items.content.multitool.MultiToolItem
import one_block.system.custom_items.content.paxel.PaxelItem
import one_block.system.custom_items.content.vacuum_hopper.VacuumHopper
import one_block.system.custom_items.content.vacuum_hopper.view.HopperFilterView
import one_block.system.custom_items.content.vacuum_hopper.view.HopperStorageView
import one_block.system.custom_items.content.vacuum_hopper.view.HopperUpgradesView
import one_block.system.custom_items.content.vacuum_hopper.view.VacuumHopperView
import one_block.system.custom_items.item.CustomItemListener
import one_block.system.custom_items.item.CustomItemRegistry
import one_block.system.custom_items.reset.CustomBlockResetHook
import one_block.system.islands.reset.IslandResetHooks
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

object CustomItemSystem {

    lateinit var plugin: JavaPlugin
    private var tickCounter = 0L

    fun init(plugin: JavaPlugin, viewFrame: ViewFrame, commandManager: PaperCommandManager) {
        this.plugin = plugin

        registerCompletions(commandManager)
        plugin.server.pluginManager.registerEvents(CustomItemListener(), plugin)
        plugin.server.pluginManager.registerEvents(CustomBlockListener(viewFrame), plugin)
        startHeldTickTask()

        registerBuiltInItems()
        registerBuiltInBlocks(viewFrame, commandManager)

        PlacedBlockManager.onInit()

        IslandResetHooks.register(CustomBlockResetHook)
    }

    private fun registerCompletions(commandManager: PaperCommandManager) {
        commandManager.commandCompletions.registerCompletion("customitems") {
            CustomItemRegistry.ids()
        }
        commandManager.commandCompletions.registerCompletion("customblocks") {
            CustomBlockRegistry.ids()
        }
    }

    private fun startHeldTickTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            tickCounter++
            for (player in Bukkit.getOnlinePlayers()) {
                tickHand(player, player.inventory.itemInMainHand)
                tickHand(player, player.inventory.itemInOffHand)
            }
        }, 1L, 1L)
    }

    private fun tickHand(player: Player, item: ItemStack) {
        val custom = CustomItemRegistry.fromItemStack(item) ?: return
        if (custom.heldTickInterval <= 0L) return
        if (tickCounter % custom.heldTickInterval != 0L) return

        runCatching { custom.onHeldTick(player, item) }
            .onFailure { plugin.logger.warning("[custom-items] held-tick de '${custom.id}' falhou: ${it.message}") }
    }

    private fun registerBuiltInItems() {
        CustomItemRegistry.register(MagnetItem())
        CustomItemRegistry.register(MultiToolItem())

        PaxelItem.all().forEach {
            CustomItemRegistry.register(it)
            Crafts.register(it.forgeRecipe())
        }
    }

    private fun registerBuiltInBlocks(viewFrame: ViewFrame, commandManager: PaperCommandManager) {
        val hopper = VacuumHopper()
        CustomBlockRegistry.register(hopper)

        viewFrame.with(
            VacuumHopperView(),
            HopperStorageView(),
            HopperFilterView(),
            HopperUpgradesView(),
        )

        commandManager.registerCommand(CustomBlockCommand())
        Crafts.register(VacuumHopper.forgeRecipe(hopper))
    }
}
