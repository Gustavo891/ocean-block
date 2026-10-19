package ocean_block.system.craft.recipe

import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.inventory.ItemStack

class Ingredient(
    val label: String,
    val amount: Int,
    val plural: String = label,
    val matches: (ItemStack) -> Boolean,
) {

    init {
        require(amount > 0) { "ingrediente '$label' precisa de quantidade positiva" }
    }

    fun labelFor(count: Int): String = if (count == 1) label else plural

    override fun toString(): String = "Ingredient($label x$amount)"

    companion object {

        fun material(material: Material, label: String, amount: Int, plural: String = label): Ingredient =
            Ingredient(label, amount, plural) { it.type == material }

        fun tag(tag: Tag<Material>, label: String, amount: Int, plural: String = label): Ingredient =
            Ingredient(label, amount, plural) { tag.isTagged(it.type) }
    }
}
