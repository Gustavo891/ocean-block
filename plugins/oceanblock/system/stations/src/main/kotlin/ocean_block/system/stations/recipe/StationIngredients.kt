package ocean_block.system.stations.recipe

import ocean_block.system.craft.recipe.Ingredient
import ocean_block.system.custom_items.CustomIds
import org.bukkit.Material
import org.bukkit.Tag

object StationIngredients {

    fun planks(amount: Int) = Ingredient.tag(Tag.PLANKS, "Tábua", amount, plural = "Tábuas")

    fun material(material: Material, label: String, amount: Int, plural: String = label) =
        Ingredient(label, amount, plural) { it.type == material && CustomIds.of(it) == null }

    fun custom(id: String, label: String, amount: Int, plural: String = label) =
        Ingredient(label, amount, plural) { CustomIds.of(it) == id }
}
