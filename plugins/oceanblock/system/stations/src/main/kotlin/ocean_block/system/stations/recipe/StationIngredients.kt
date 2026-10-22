package ocean_block.system.stations.recipe

import ocean_block.system.craft.recipe.Ingredient
import ocean_block.system.custom_items.CustomIds
import ocean_block.system.stations.sieve.SieveBlock
import ocean_block.system.stations.smelter.SmelterBlock
import org.bukkit.Material
import org.bukkit.Tag

object StationIngredients {

    fun planks(amount: Int) = Ingredient.tag(Tag.PLANKS, "Tábua", amount, plural = "Tábuas", icon = "oak_planks")

    fun material(material: Material, label: String, amount: Int, plural: String = label) =
        Ingredient(label, amount, plural, material.name.lowercase()) { it.type == material && CustomIds.of(it) == null }

    fun custom(id: String, label: String, amount: Int, plural: String = label) =
        Ingredient(label, amount, plural, baseMaterial(id)) { CustomIds.of(it) == id }

    private fun baseMaterial(id: String): String? =
        (SieveBlock.of(id)?.material ?: SmelterBlock.of(id)?.material)?.name?.lowercase()
}
