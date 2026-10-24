package ocean_block.system.machines.recipe

import ocean_block.system.craft.recipe.Ingredient
import ocean_block.system.craft.recipe.OceanRecipe
import ocean_block.system.craft.recipe.RecipeCategory
import ocean_block.system.craft.recipe.Recipes
import ocean_block.system.machines.MachineBlock
import ocean_block.system.machines.MachineIds
import ocean_block.system.machines.furnace.FurnaceBlock
import ocean_block.system.machines.generator.GeneratorBlock
import ocean_block.system.machines.miner.MinerBlock
import ocean_block.system.machines.pickaxe.SmelterPickaxeItem
import ocean_block.system.machines.solar.SolarBlock
import ocean_block.system.stations.recipe.StationIngredients.custom
import ocean_block.system.stations.recipe.StationIngredients.material
import ocean_block.system.stations.smelter.SmelterTier
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object MachineRecipes {

    private fun iron(n: Int) = material(Material.IRON_INGOT, "Lingote de ferro", n, "Lingotes de ferro")
    private fun gold(n: Int) = material(Material.GOLD_INGOT, "Lingote de ouro", n, "Lingotes de ouro")
    private fun copper(n: Int) = material(Material.COPPER_INGOT, "Lingote de cobre", n, "Lingotes de cobre")
    private fun redstone(n: Int) = material(Material.REDSTONE, "Redstone", n)
    private fun lapis(n: Int) = material(Material.LAPIS_LAZULI, "Lápis-lazúli", n)
    private fun coal(n: Int) = material(Material.COAL, "Carvão", n, "Carvões")

    fun register() {
        Recipes.register(
            recipe(MachineIds.PICKAXE, "Picareta fundidora", "Minério quebrado já sai em lingote.", RecipeCategory.FERRAMENTAS,
                { SmelterPickaxeItem.build() }, iron(4), gold(2), coal(8)),
        )
        Recipes.register(
            block(GeneratorBlock, "Gerador de energia", "Queima combustível e alimenta máquinas a até 8 blocos.",
                custom(SmelterTier.BASIC.blockId, "Fundição", 1, "Fundições"), iron(8), copper(6), redstone(4)),
        )
        Recipes.register(
            block(SolarBlock, "Painel solar", "Gera um pouco de energia de dia, sem combustível.",
                copper(6), lapis(4), redstone(2), iron(2)),
        )
        Recipes.register(
            block(FurnaceBlock, "Fornalha elétrica", "Derrete 4 itens ao mesmo tempo, 4 vezes mais rápido.",
                custom(SmelterTier.IMPROVED.blockId, "Fundição rápida", 1, "Fundições rápidas"), iron(10), gold(4), redstone(8)),
        )
        Recipes.register(
            block(MinerBlock, "Minerador", "Quebra sozinho a pedra e o minério logo abaixo.",
                iron(14), gold(4), redstone(8)),
        )
    }

    private fun block(machine: MachineBlock, name: String, description: String, vararg ingredients: Ingredient) =
        recipe(machine.id, name, description, RecipeCategory.MAQUINAS, { machine.buildItem() }, *ingredients)

    private fun recipe(
        id: String,
        name: String,
        description: String,
        category: RecipeCategory,
        result: () -> ItemStack,
        vararg ingredients: Ingredient,
    ) = OceanRecipe(
        id = id,
        name = name,
        description = description,
        category = category,
        result = result,
        ingredients = ingredients.toList(),
        research = id,
    )
}
