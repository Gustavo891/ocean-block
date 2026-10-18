package one_block.system.minions

import co.aikar.commands.PaperCommandManager
import me.devnatan.inventoryframework.ViewFrame
import one_block.libs.instance.Instance
import one_block.system.craft.CraftCategory
import one_block.system.craft.Crafts
import one_block.system.craft.forge.ForgeRecipe
import one_block.system.craft.forge.ForgeRequirement
import one_block.system.craft.forge.TexturaIngredients
import one_block.system.islands.reset.IslandResetHooks
import one_block.system.minions.reset.MinionResetHook
import one_block.system.minions.command.FragmentCommand
import one_block.system.minions.command.FuelCommand
import one_block.system.minions.command.MinionCommand
import one_block.system.minions.enums.MinionType
import one_block.system.minions.listener.PlayerListener
import one_block.system.minions.listener.TerminatorKillListener
import one_block.system.minions.model.MinionModel
import one_block.system.minions.model.MinionModelListener
import one_block.system.minions.placed_minion.PlacedMinionManager
import one_block.system.minions.view.FilterView
import one_block.system.minions.view.MinionView
import one_block.system.minions.view.UpgradesView
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object MinionsSystem {

    fun init(viewFrame: ViewFrame, paperCommandManager: PaperCommandManager) {
        PlacedMinionManager.onInit()

        viewFrame.with(MinionView())
        viewFrame.with(UpgradesView())
        viewFrame.with(FilterView())

        setupCommands(viewFrame, paperCommandManager)
        setupListeners(viewFrame)
        setupRecipes()

        IslandResetHooks.register(MinionResetHook)
    }

    private fun setupCommands(viewFrame: ViewFrame, paperCommandManager: PaperCommandManager) {
        paperCommandManager.registerCommand(MinionCommand())
        paperCommandManager.registerCommand(FuelCommand())
        paperCommandManager.registerCommand(FragmentCommand())
    }

    private fun setupListeners(viewFrame: ViewFrame) = with(Instance.server.pluginManager){
		registerEvents(PlayerListener(viewFrame), Instance)
        registerEvents(TerminatorKillListener(), Instance)
        if (MinionModel.isAvailable()) {
            registerEvents(MinionModelListener(viewFrame), Instance)
        }
    }

    private fun setupRecipes() {
        registerMinionRecipe(
            id = "farmer_minion",
            prettyName = "Minion Fazendeiro",
            type = MinionType.Farmer,
            fragmentos = 5,
            lingotes = 8,
            ferramenta = ForgeRequirement("Enxada de ferro", ItemStack(Material.IRON_HOE), 1),
            category = CraftCategory.AGRICULTURA,
        )

        registerMinionRecipe(
            id = "miner_minion",
            prettyName = "Minion Minerador",
            type = MinionType.Miner,
            fragmentos = 8,
            lingotes = 12,
            ferramenta = ForgeRequirement("Picareta de diamante", ItemStack(Material.DIAMOND_PICKAXE), 1),
            category = CraftCategory.MINERACAO,
        )

        registerMinionRecipe(
            id = "terminator_minion",
            prettyName = "Minion Exterminador",
            type = MinionType.Terminator,
            fragmentos = 10,
            lingotes = 16,
            ferramenta = ForgeRequirement("Espada de diamante", ItemStack(Material.DIAMOND_SWORD), 1),
            category = CraftCategory.COMBATE,
        )
    }

    private fun registerMinionRecipe(
        id: String,
        prettyName: String,
        type: MinionType,
        fragmentos: Int,
        lingotes: Int,
        ferramenta: ForgeRequirement,
        category: CraftCategory,
    ) {
        val platina = TexturaIngredients.requirement("Lingote de Platina", PLATINUM_INGOT, lingotes)
        if (platina == null) {
            Instance.logger.warning("[minions] receita '$id' nao registrada: falta o item '$PLATINUM_INGOT'.")
            return
        }

        val item = Minions.createMinionItem(type)

        Crafts.register(
            ForgeRecipe(
                id = id,
                prettyName = prettyName,
                icon = item,
                result = item,
                requirements = listOf(
                    ForgeRequirement("Fragmento de Minion", Minions.machinePart(), fragmentos),
                    platina,
                    ferramenta,
                ),
                category = category,
            )
        )
    }

    private const val PLATINUM_INGOT = "platinum_ingot"
}
