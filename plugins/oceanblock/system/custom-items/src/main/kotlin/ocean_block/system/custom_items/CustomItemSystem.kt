package ocean_block.system.custom_items

import co.aikar.commands.PaperCommandManager
import me.devnatan.inventoryframework.ViewFrame
import ocean_block.system.custom_items.block.CustomBlockCommand
import ocean_block.system.custom_items.block.BlockItemUpgradeListener
import ocean_block.system.custom_items.block.CustomBlockListener
import ocean_block.system.custom_items.block.furniture.FurnitureBreakListener
import ocean_block.system.custom_items.block.CustomBlockRegistry
import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.custom_items.content.multitool.MultiToolItem
import ocean_block.system.custom_items.content.paxel.PaxelItem
import ocean_block.system.custom_items.content.paxel.PaxelRecipes
import ocean_block.system.custom_items.item.CustomItemListener
import ocean_block.system.custom_items.item.CustomItemRegistry
import ocean_block.system.custom_items.reset.CustomBlockResetHook
import ocean_block.system.islands.reset.IslandResetHooks
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
        plugin.server.pluginManager.registerEvents(FurnitureBreakListener(), plugin)
        plugin.server.pluginManager.registerEvents(BlockItemUpgradeListener(), plugin)
        startHeldTickTask()

        registerBuiltInItems()
        registerBuiltInBlocks(commandManager)

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
        CustomItemRegistry.register(MultiToolItem())

        val paxels = PaxelItem.all()
        paxels.forEach { CustomItemRegistry.register(it) }

        val paxelRecipes = PaxelRecipes(plugin)
        paxelRecipes.register(paxels)
        plugin.server.pluginManager.registerEvents(paxelRecipes, plugin)
    }

    private fun registerBuiltInBlocks(commandManager: PaperCommandManager) {
        commandManager.registerCommand(CustomBlockCommand())
    }
}
