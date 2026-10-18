package one_block.system.repair

import co.aikar.commands.PaperCommandManager
import one_block.libs.item_builder.itemStack
import one_block.system.craft.CraftCategory
import one_block.system.craft.Crafts
import one_block.system.craft.forge.ForgeRecipe
import one_block.system.craft.forge.ForgeRequirement
import one_block.system.mobdrops.MobDrops
import one_block.system.repair.command.RepairCommand
import one_block.system.repair.enums.Fragment
import one_block.system.repair.enums.Repair
import one_block.system.repair.listener.PlayerListener
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.plugin.java.JavaPlugin

object RepairsSystem {

    fun init(plugin: JavaPlugin, commandManager: PaperCommandManager) {
        setupListeners(plugin)
        setupCommands(commandManager)

        setupMobDrops()
        setupDefaultRecipes()
    }

    private fun setupListeners(plugin: JavaPlugin) = with(plugin.server.pluginManager) {
        registerEvents(PlayerListener(), plugin)
    }

    private fun setupCommands(commandManager: PaperCommandManager) {
        commandManager.registerCommand(RepairCommand())
    }

    private fun setupMobDrops() {
        for (fragment in Fragment.entries) {
            MobDrops.addDrop(
                EntityType.ZOMBIE,
                { Repairs.createFragmentItem(fragment) },
                0.005
            )

            MobDrops.addDrop(
                EntityType.CREEPER,
                { Repairs.createFragmentItem(fragment) } ,
                0.005
            )

            MobDrops.addDrop(
                EntityType.SKELETON,
                { Repairs.createFragmentItem(fragment) } ,
                0.005
            )
        }
    }

    private fun setupDefaultRecipes() {
        Crafts.register(
            ForgeRecipe(
                "repair_1",
                "Repair I",
                Repairs.createRepairItem(Repair.I),
                Repairs.createRepairItem(Repair.I),
                listOf(
                    ForgeRequirement("Bigorna", itemStack(Material.ANVIL) {}, 1),
                    ForgeRequirement("Fragmento de reparo I", Repairs.createRepairItem(Repair.I), 10)  {
                        Repairs.getFragmentByItem(it) == Fragment.I.name
                    },
                ),
                category = CraftCategory.ESPECIAIS,
            )
        )

        Crafts.register(
            ForgeRecipe(
                "repair_2",
                "Repair II",
                Repairs.createRepairItem(Repair.II),
                Repairs.createRepairItem(Repair.II),
                listOf(
                    ForgeRequirement("Bigorna", itemStack(Material.ANVIL) {}, 1),
                    ForgeRequirement("Fragmento de reparo II", Repairs.createRepairItem(Repair.II), 10) {
                        Repairs.getFragmentByItem(it) == Fragment.II.name
                    },
                ),
                category = CraftCategory.ESPECIAIS,
            )
        )

        Crafts.register(
            ForgeRecipe(
                "repair_3",
                "Repair III",
                Repairs.createRepairItem(Repair.III),
                Repairs.createRepairItem(Repair.III),
                listOf(
                    ForgeRequirement("Bigorna", itemStack(Material.ANVIL) {}, 1),
                    ForgeRequirement("Fragmento de reparo III", Repairs.createRepairItem(Repair.III), 10) {
                        Repairs.getFragmentByItem(it) == Fragment.III.name
                    }
                ),
                category = CraftCategory.ESPECIAIS,
            )
        )
    }
}
