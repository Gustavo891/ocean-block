package one_block.system.craft

import co.aikar.commands.PaperCommandManager
import me.devnatan.inventoryframework.ViewFrame
import one_block.libs.instance.Instance
import one_block.libs.item_builder.itemStack
import one_block.system.craft.command.CraftCommand
import one_block.system.craft.forge.ForgeRecipe
import one_block.system.craft.forge.ForgeRequirement
import one_block.system.craft.view.CraftView
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object CraftSystem {

    fun init(viewFrame: ViewFrame, paperCommandManager: PaperCommandManager) {
        viewFrame.with(CraftView())
        setupCommands(viewFrame, paperCommandManager)
        setupListeners()

        setupDefaultRecipes()
    }

    private fun setupCommands(viewFrame: ViewFrame, paperCommandManager: PaperCommandManager) {
        paperCommandManager.registerCommand(CraftCommand(viewFrame))
    }

    private fun setupListeners() = with(Instance.server.pluginManager) {
        //    registerEvents(PlayerListener(), Instance)
    }

    private fun setupDefaultRecipes() {
        Crafts.register(
            ForgeRecipe(
                "leather",
                "Couro",
                itemStack(Material.LEATHER) { },
                ItemStack.of(Material.LEATHER),
                listOf(
                    ForgeRequirement("Carne podre", ItemStack.of(Material.ROTTEN_FLESH), 9),
                )
            )
        )
    }
}