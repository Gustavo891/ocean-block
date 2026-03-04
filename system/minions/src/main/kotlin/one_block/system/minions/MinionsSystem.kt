package one_block.system.minions

import co.aikar.commands.PaperCommandManager
import me.devnatan.inventoryframework.ViewFrame
import one_block.libs.instance.Instance
import one_block.libs.item_builder.itemStack
import one_block.system.minions.command.FuelCommand
import one_block.system.minions.command.MinionCommand
import one_block.system.minions.command.MinionsCommand
import one_block.system.minions.forge.ForgeRecipe
import one_block.system.minions.forge.ForgeRequirement
import one_block.system.minions.listener.PlayerListener
import one_block.system.minions.placed_minion.PlacedMinionManager
import one_block.system.minions.view.MinionsView
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object MinionsSystem {

    val recipes = mutableListOf<ForgeRecipe>()

    fun init(viewFrame: ViewFrame, paperCommandManager: PaperCommandManager) {
        PlacedMinionManager.onInit()

        setupCommands(viewFrame, paperCommandManager)
        setupListeners()
        viewFrame.with(MinionsView())

        val farmerRecipe = ForgeRecipe(
            id = "farmer_minion",
            icon = itemStack(Material.STONE) { },
            result = itemStack(Material.WHEAT) { },
            requirements = listOf(
                ForgeRequirement("Fragmento I", MinionItems.machinePart(), 5),
                ForgeRequirement("Enxada de ferro", ItemStack(Material.IRON_HOE), 1)
            )
        )

        recipes.add(farmerRecipe)
    }

    private fun setupCommands(viewFrame: ViewFrame, paperCommandManager: PaperCommandManager) {
        paperCommandManager.registerCommand(MinionCommand())
        paperCommandManager.registerCommand(FuelCommand())
        paperCommandManager.registerCommand(MinionsCommand(viewFrame))
    }

    private fun setupListeners() = with(Instance.server.pluginManager){
        registerEvents(PlayerListener(), Instance)
    }
}