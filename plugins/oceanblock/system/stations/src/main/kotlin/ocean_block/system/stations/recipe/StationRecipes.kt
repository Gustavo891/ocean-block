package ocean_block.system.stations.recipe

import ocean_block.system.craft.recipe.Ingredient
import ocean_block.system.craft.recipe.OceanRecipe
import ocean_block.system.craft.recipe.RecipeCategory
import ocean_block.system.craft.recipe.Recipes
import ocean_block.system.stations.recipe.StationIngredients.custom
import ocean_block.system.stations.recipe.StationIngredients.material
import ocean_block.system.stations.recipe.StationIngredients.planks
import ocean_block.system.stations.smelter.SmelterBlock
import ocean_block.system.stations.smelter.SmelterTier
import org.bukkit.Material

object StationRecipes {

    fun registerSmelters() {
        Recipes.register(
            smelter(SmelterTier.BASIC, "Fundição", "Derrete minério bruto em lingotes.", 1,
                material(Material.CLAY_BALL, "Argila", 8, "Argilas"), planks(4)),
        )
        Recipes.register(
            smelter(SmelterTier.IMPROVED, "Fundição rápida", "Duas vezes mais rápida, com chance de lingote extra.", 8,
                custom(SmelterTier.BASIC.blockId, "Fundição", 1, "Fundições"),
                material(Material.IRON_INGOT, "Lingote de ferro", 6, "Lingotes de ferro"),
                material(Material.COPPER_INGOT, "Lingote de cobre", 4, "Lingotes de cobre")),
        )
        Recipes.register(
            smelter(SmelterTier.HOPPER, "Fundição com funil", "Recebe e entrega por funis e baús.", 15,
                custom(SmelterTier.IMPROVED.blockId, "Fundição rápida", 1, "Fundições rápidas"),
                material(Material.HOPPER, "Funil", 2, "Funis"),
                material(Material.REDSTONE, "Redstone", 2)),
        )
    }

    private fun smelter(tier: SmelterTier, name: String, description: String, level: Int, vararg ingredients: Ingredient) =
        OceanRecipe(
            id = tier.blockId,
            name = name,
            description = description,
            category = RecipeCategory.PROCESSAMENTO,
            result = { SmelterBlock.all.first { it.tier == tier }.buildItem() },
            ingredients = ingredients.toList(),
            level = level,
        )
}
