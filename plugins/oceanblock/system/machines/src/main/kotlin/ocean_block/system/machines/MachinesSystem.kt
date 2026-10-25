package ocean_block.system.machines

import ocean_block.system.custom_items.block.CustomBlockRegistry
import ocean_block.system.custom_items.item.CustomItemRegistry
import ocean_block.system.machines.energy.EnergyBeams
import ocean_block.system.machines.energy.EnergyEffects
import ocean_block.system.machines.energy.EnergyIndex
import org.bukkit.event.player.PlayerQuitEvent
import ocean_block.system.machines.furnace.FurnaceBlock
import ocean_block.system.machines.furnace.FurnaceDialog
import ocean_block.system.machines.furnace.FurnaceHopperGuard
import ocean_block.system.machines.furnace.FurnaceMenu
import ocean_block.system.machines.furnace.FurnaceRecipes
import ocean_block.system.machines.generator.GeneratorBlock
import ocean_block.system.machines.generator.GeneratorDialog
import ocean_block.system.machines.generator.GeneratorMenu
import ocean_block.system.machines.miner.DrillItem
import ocean_block.system.machines.miner.MinerBlock
import ocean_block.system.machines.miner.MinerCracks
import ocean_block.system.machines.miner.MinerDisplay
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
    private const val RADIUS_TICKS = 10L
    private val SOURCE_MATERIALS = setOf(Material.NOTE_BLOCK, Material.DAYLIGHT_DETECTOR)

    fun init(plugin: JavaPlugin, viewFrame: ViewFrame) {
        val machines = listOf(GeneratorBlock, SolarBlock, FurnaceBlock, MinerBlock)
        machines.forEach(CustomBlockRegistry::register)
        plugin.server.pluginManager.registerEvents(MachineInteractListener(viewFrame, machines.associateBy { it.id }), plugin)
        CustomItemRegistry.register(SmelterPickaxeItem)
        DrillItem.all.forEach(CustomItemRegistry::register)
        MachineRecipes.register()
        viewFrame.with(GeneratorMenu(), FurnaceMenu(), MinerMenu())
        GeneratorDialog.registerIcons()
        FurnaceRecipes.load()
        FurnaceDialog.registerIcons()
        plugin.server.pluginManager.registerEvents(IndexListener(plugin), plugin)
        plugin.server.pluginManager.registerEvents(FurnaceHopperGuard(), plugin)
        plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            EnergyIndex.rebuild()
            MinerDisplay.cleanup()
        }, 20L, REBUILD_TICKS)
        plugin.server.scheduler.runTaskTimer(plugin, Runnable { EnergyEffects.drawRadii() }, 20L, RADIUS_TICKS)
        plugin.server.scheduler.runTaskTimer(plugin, Runnable { EnergyBeams.refresh() }, 20L, EnergyBeams.REFRESH_TICKS)
        plugin.server.scheduler.runTaskTimer(plugin, Runnable { MinerCracks.refresh() }, 20L, MinerCracks.REFRESH_TICKS)
    }

    fun onDisable() {
        MinerDisplay.removeAll()
        MinerCracks.clearAll()
        EnergyBeams.removeAll()
    }

    private class IndexListener(private val plugin: JavaPlugin) : Listener {

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        fun onPlace(event: BlockPlaceEvent) {
            if (CustomIds.of(event.itemInHand) in MachineIds.sources) scheduleRebuild()
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        fun onBreak(event: BlockBreakEvent) {
            if (event.block.type in SOURCE_MATERIALS) scheduleRebuild()
        }

        @EventHandler
        fun onQuit(event: PlayerQuitEvent) = EnergyEffects.forget(event.player)

        private fun scheduleRebuild() {
            plugin.server.scheduler.runTask(plugin, Runnable { EnergyIndex.rebuild() })
        }
    }
}
