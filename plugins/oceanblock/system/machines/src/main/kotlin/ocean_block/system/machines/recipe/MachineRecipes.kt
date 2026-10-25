package ocean_block.system.machines.recipe

import ocean_block.system.craft.recipe.Ingredient
import ocean_block.system.craft.recipe.OceanRecipe
import ocean_block.system.craft.recipe.RecipeCategory
import ocean_block.system.craft.recipe.Recipes
import ocean_block.system.machines.MachineBlock
import ocean_block.system.machines.MachineIds
import ocean_block.system.machines.furnace.FurnaceBlock
import ocean_block.system.machines.generator.GeneratorBlock
import ocean_block.system.machines.miner.DrillItem
import ocean_block.system.machines.miner.DrillTier
import ocean_block.system.machines.miner.MinerBlock
import ocean_block.system.machines.pickaxe.SmelterPickaxeItem
import ocean_block.system.machines.solar.SolarBlock
import ocean_block.system.machines.recipe.MachineIngredients.custom
import ocean_block.system.machines.recipe.MachineIngredients.material
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object MachineRecipes {

    private fun iron(n: Int) = material(Material.IRON_INGOT, "Lingote de ferro", n, "Lingotes de ferro")
    private fun gold(n: Int) = material(Material.GOLD_INGOT, "Lingote de ouro", n, "Lingotes de ouro")
    private fun copper(n: Int) = material(Material.COPPER_INGOT, "Lingote de cobre", n, "Lingotes de cobre")
    private fun redstone(n: Int) = material(Material.REDSTONE, "Redstone", n)
    private fun lapis(n: Int) = material(Material.LAPIS_LAZULI, "Lápis-lazúli", n)
    private fun coal(n: Int) = material(Material.COAL, "Carvão", n, "Carvões")
    private fun diamond(n: Int) = material(Material.DIAMOND, "Diamante", n, "Diamantes")
    private fun furnace(n: Int) = material(Material.FURNACE, "Fornalha", n, "Fornalhas")
    private fun drill(tier: DrillTier) = custom(tier.itemId.orEmpty(), "Broca de ${tier.label}", 1, "Brocas de ${tier.label}")

    fun register() {
        Recipes.register(
            recipe(MachineIds.PICKAXE, "Picareta fundidora", "Minério quebrado já sai em lingote.", RecipeCategory.FERRAMENTAS, 12,
                { SmelterPickaxeItem.build() }, iron(4), gold(2), coal(8)),
        )
        Recipes.register(
            block(GeneratorBlock, 18, "Gerador de energia", "Queima combustível e alimenta máquinas a até 8 blocos.",
                furnace(1), iron(8), copper(6), redstone(4)),
        )
        Recipes.register(
            block(SolarBlock, 20, "Painel solar", "Gera um pouco de energia de dia, sem combustível.",
                copper(6), lapis(4), redstone(2), iron(2)),
        )
        Recipes.register(
            block(FurnaceBlock, 22, "Fornalha elétrica", "Derrete 4 itens ao mesmo tempo, 4 vezes mais rápido.",
                furnace(1), iron(10), gold(4), redstone(8)),
        )
        Recipes.register(
            block(MinerBlock, 25, "Minerador", "Quebra sozinho a pedra e o minério logo abaixo.",
                iron(14), gold(4), redstone(8)),
        )
        drillRecipe(DrillTier.GOLD, 28, "A broca de ouro quebra um bloco a cada 1,5 s.",
            material(Material.GOLD_BLOCK, "Bloco de ouro", 2, "Blocos de ouro"), iron(4), redstone(4))
        drillRecipe(DrillTier.DIAMOND, 32, "A broca de diamante quebra um bloco por segundo.",
            drill(DrillTier.GOLD), diamond(6), redstone(4))
        drillRecipe(DrillTier.OBSIDIAN, 36, "A broca de obsidiana quebra um bloco a cada 0,75 s.",
            drill(DrillTier.DIAMOND), material(Material.OBSIDIAN, "Obsidiana", 8, "Obsidianas"), diamond(2))
        drillRecipe(DrillTier.NETHERITE, 40, "A broca de netherite quebra um bloco a cada 0,5 s.",
            drill(DrillTier.OBSIDIAN), material(Material.NETHERITE_INGOT, "Lingote de netherite", 2, "Lingotes de netherite"))
    }

    private fun drillRecipe(tier: DrillTier, level: Int, description: String, vararg ingredients: Ingredient) {
        val item = DrillItem.of(tier) ?: return
        Recipes.register(
            recipe(item.id, "Broca de ${tier.label}", description, RecipeCategory.MAQUINAS, level, { item.build() }, *ingredients),
        )
    }

    private fun block(machine: MachineBlock, level: Int, name: String, description: String, vararg ingredients: Ingredient) =
        recipe(machine.id, name, description, RecipeCategory.MAQUINAS, level, { machine.buildItem() }, *ingredients)

    private fun recipe(
        id: String,
        name: String,
        description: String,
        category: RecipeCategory,
        level: Int,
        result: () -> ItemStack,
        vararg ingredients: Ingredient,
    ) = OceanRecipe(
        id = id,
        name = name,
        description = description,
        category = category,
        result = result,
        ingredients = ingredients.toList(),
        level = level,
    )
}
