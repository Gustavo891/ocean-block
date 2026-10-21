package ocean_block.system.stations.recipe

import ocean_block.system.craft.recipe.OceanRecipe
import ocean_block.system.craft.recipe.RecipeCategory
import ocean_block.system.craft.recipe.Recipes
import ocean_block.system.stations.recipe.StationIngredients.custom
import ocean_block.system.stations.recipe.StationIngredients.material
import ocean_block.system.stations.recipe.StationIngredients.planks
import ocean_block.system.stations.sieve.SieveBlock
import ocean_block.system.stations.sieve.SieveTier
import org.bukkit.Material

object StationRecipes {

    fun registerSieves() {
        Recipes.register(
            sieve(SieveTier.WOOD, "Peneira", "Peneira areia, cascalho e terra atrás de minério.", null,
                planks(4), material(Material.STRING, "Linha", 2, "Linhas"), material(Material.STICK, "Graveto", 2, "Gravetos")),
        )
        Recipes.register(
            sieve(SieveTier.IRON, "Peneira de ferro", "Menos etapas e saídas melhores, como redstone e diamante.", SieveTier.IRON.blockId,
                custom(SieveTier.WOOD.blockId, "Peneira", 1, "Peneiras"), material(Material.IRON_INGOT, "Lingote de ferro", 4, "Lingotes de ferro")),
        )
        Recipes.register(
            sieve(SieveTier.DIAMOND, "Peneira de diamante", "A peneira mais rápida, com as melhores chances.", SieveTier.DIAMOND.blockId,
                custom(SieveTier.IRON.blockId, "Peneira de ferro", 1, "Peneiras de ferro"),
                material(Material.DIAMOND, "Diamante", 2, "Diamantes"),
                material(Material.GOLD_INGOT, "Lingote de ouro", 4, "Lingotes de ouro")),
        )
        Recipes.register(
            sieve(SieveTier.AUTOMATIC, "Peneira automática", "Peneira sozinha entre um baú acima e um abaixo.", SieveTier.AUTOMATIC.blockId,
                custom(SieveTier.IRON.blockId, "Peneira de ferro", 1, "Peneiras de ferro"),
                material(Material.IRON_INGOT, "Lingote de ferro", 4, "Lingotes de ferro"),
                material(Material.REDSTONE, "Redstone", 4),
                material(Material.HOPPER, "Funil", 1, "Funis")),
        )
    }

    private fun sieve(tier: SieveTier, name: String, description: String, research: String?, vararg ingredients: ocean_block.system.craft.recipe.Ingredient) =
        OceanRecipe(
            id = tier.blockId,
            name = name,
            description = description,
            category = RecipeCategory.PROCESSAMENTO,
            result = { SieveBlock.all.first { it.tier == tier }.buildItem() },
            ingredients = ingredients.toList(),
            research = research,
        )
}
