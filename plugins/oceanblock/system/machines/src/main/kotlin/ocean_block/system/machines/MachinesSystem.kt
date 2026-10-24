package ocean_block.system.machines

import ocean_block.system.custom_items.block.CustomBlockRegistry
import ocean_block.system.custom_items.item.CustomItemRegistry
import ocean_block.system.machines.energy.EnergyIndex
import ocean_block.system.machines.furnace.FurnaceBlock
import ocean_block.system.machines.furnace.FurnaceMenu
import ocean_block.system.machines.furnace.FurnaceRecipes
import ocean_block.system.machines.generator.GeneratorBlock
import ocean_block.system.machines.generator.GeneratorMenu
import ocean_block.system.machines.miner.MinerBlock
import ocean_block.system.machines.miner.MinerMenu
import ocean_block.system.machines.pickaxe.SmelterPickaxeItem
import ocean_block.system.machines.recipe.MachineRecipes
import ocean_block.system.machines.solar.SolarBlock
import me.devnatan.inventoryframework.ViewFrame
import ocean_block.system.custom_items.CustomIds
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.plugin.java.JavaPlugin

object MachinesSystem {

    private const val REBUILD_TICKS = 100L

    fun init(plugin: JavaPlugin, viewFrame: ViewFrame) {
        listOf(GeneratorBlock, SolarBlock, FurnaceBlock, MinerBlock).forEach(CustomBlockRegistry::register)
        CustomItemRegistry.register(SmelterPickaxeItem)
        MachineRecipes.register()
        viewFrame.with(GeneratorMenu(), FurnaceMenu(), MinerMenu())
        FurnaceRecipes.load()
        plugin.server.pluginManager.registerEvents(IndexListener(plugin), plugin)
        plugin.server.scheduler.runTaskTimer(plugin, Runnable { EnergyIndex.rebuild() }, 20L, REBUILD_TICKS)
    }

    private class IndexListener(private val plugin: JavaPlugin) : Listener {

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        fun onPlace(event: BlockPlaceEvent) {
            if (CustomIds.of(event.itemInHand) in MachineIds.sources) scheduleRebuild()
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        fun onBreak(event: BlockBreakEvent) {
            if (event.block.type == Material.NOTE_BLOCK) scheduleRebuild()
        }

        private fun scheduleRebuild() {
            plugin.server.scheduler.runTask(plugin, Runnable { EnergyIndex.rebuild() })
        }
    }
}
