package one_block.system.craft

import co.aikar.commands.PaperCommandManager
import me.devnatan.inventoryframework.ViewFrame
import one_block.libs.instance.Instance
import one_block.libs.item_builder.itemStack
import one_block.system.craft.command.CraftCommand
import one_block.system.craft.command.ForgeCommand
import one_block.system.craft.view.CraftCategoriesView
import one_block.system.craft.view.ForgeCategoriesView
import one_block.system.craft.forge.ForgeRecipe
import one_block.system.craft.forge.ForgeRequirement
import one_block.system.craft.forge.TexturaIngredients
import one_block.system.craft.view.CraftView
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object CraftSystem {

    fun init(viewFrame: ViewFrame, paperCommandManager: PaperCommandManager) {
        viewFrame.with(CraftView(), CraftCategoriesView(), ForgeCategoriesView())
        setupCommands(viewFrame, paperCommandManager)
        setupListeners()

        setupDefaultRecipes()
        setupPlatinumRecipes()
    }

    private fun setupCommands(viewFrame: ViewFrame, paperCommandManager: PaperCommandManager) {
        paperCommandManager.registerCommand(CraftCommand(viewFrame))
        paperCommandManager.registerCommand(ForgeCommand(viewFrame))
    }

    private fun setupListeners() = with(Instance.server.pluginManager) {
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

    private fun setupPlatinumRecipes() {
        val lingote = TexturaIngredients.item(PLATINUM_INGOT) ?: return
        val bruta = TexturaIngredients.requirement("Platina Bruta", PLATINUM_RAW, RAW_POR_LINGOTE) ?: return

        Crafts.register(
            ForgeRecipe(
                id = "platinum_ingot",
                prettyName = "Lingote de Platina",
                icon = lingote,
                result = lingote,
                requirements = listOf(bruta),
                category = CraftCategory.MINERIOS,
            )
        )
    }

    private const val PLATINUM_RAW = "platinum_raw"
    private const val PLATINUM_INGOT = "platinum_ingot"
    private const val RAW_POR_LINGOTE = 5
}
