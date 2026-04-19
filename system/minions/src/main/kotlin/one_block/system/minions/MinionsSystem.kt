package one_block.system.minions

import co.aikar.commands.PaperCommandManager
import me.devnatan.inventoryframework.ViewFrame
import one_block.libs.instance.Instance
import one_block.libs.item_builder.itemStack
import one_block.system.craft.Crafts
import one_block.system.craft.forge.ForgeRecipe
import one_block.system.craft.forge.ForgeRequirement
import one_block.system.minions.command.FragmentCommand
import one_block.system.minions.command.FuelCommand
import one_block.system.minions.command.MinionCommand
import one_block.system.minions.enums.MinionType
import one_block.system.minions.listener.PlayerListener
import one_block.system.minions.placed_minion.PlacedMinionManager
import one_block.system.minions.view.MinionView
import one_block.system.minions.view.UpgradesView
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object MinionsSystem {

    fun init(viewFrame: ViewFrame, paperCommandManager: PaperCommandManager) {
        PlacedMinionManager.onInit()

        viewFrame.with(MinionView())
        viewFrame.with(UpgradesView())

        setupCommands(viewFrame, paperCommandManager)
        setupListeners(viewFrame)
        setupRecipes()
    }

    private fun setupCommands(viewFrame: ViewFrame, paperCommandManager: PaperCommandManager) {
        paperCommandManager.registerCommand(MinionCommand())
        paperCommandManager.registerCommand(FuelCommand())
        paperCommandManager.registerCommand(FragmentCommand())
    }

    private fun setupListeners(viewFrame: ViewFrame) = with(Instance.server.pluginManager){
		registerEvents(PlayerListener(viewFrame), Instance)
    }

    private fun setupRecipes() {
        val farmerRecipe = ForgeRecipe(
            id = "farmer_minion",
            prettyName = "Farmer",
            icon = itemStack(Material.FARMLAND) { },
            result = Minions.createMinionItem(MinionType.Farmer),
            requirements = listOf(
                ForgeRequirement("Fragmento I", Minions.machinePart(), 5),
                ForgeRequirement("Enxada de ferro", ItemStack(Material.IRON_HOE), 1)
            )
        )

        val minerRecipe = ForgeRecipe(
            id = "miner_minion",
            prettyName = "Miner",
            icon = itemStack(Material.DIAMOND_PICKAXE) { },
            result = Minions.createMinionItem(MinionType.Miner),
            requirements = listOf(
                ForgeRequirement("Fragmento I", Minions.machinePart(), 10),
                ForgeRequirement("Picareta de diamante", ItemStack(Material.DIAMOND_PICKAXE), 1)
            )
        )

        Crafts.register(ForgeRecipe(
            id = "craft_minion",
            prettyName = "Minion de Mineração" ,
            icon = Minions.createMinionItem(MinionType.Miner),
            result = Minions.createMinionItem(MinionType.Miner),
            requirements = listOf(
                ForgeRequirement("Barra de Netherite", itemStack(Material.NETHERITE_INGOT) {}, 9),
                ForgeRequirement("Núcleo Pesado", itemStack(Material.HEAVY_CORE) {}, 1),
                ForgeRequirement("Fragmento de Minion I", Minions.machinePart(), 8)
            )

        ))

    }
}