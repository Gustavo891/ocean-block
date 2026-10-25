package ocean_block.system.machines.recipe

import ocean_block.system.craft.recipe.Ingredient
import ocean_block.system.custom_items.CustomIds
import org.bukkit.Material

object MachineIngredients {

    fun material(material: Material, label: String, amount: Int, plural: String = label) =
        Ingredient(label, amount, plural, material.name.lowercase()) { it.type == material && CustomIds.of(it) == null }

    fun custom(id: String, label: String, amount: Int, plural: String = label) =
        Ingredient(label, amount, plural, null) { CustomIds.of(it) == id }
}
