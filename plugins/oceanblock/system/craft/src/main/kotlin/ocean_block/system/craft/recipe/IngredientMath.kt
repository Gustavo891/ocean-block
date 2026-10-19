package ocean_block.system.craft.recipe

import org.bukkit.inventory.ItemStack

object IngredientMath {

    private val amountOf: (ItemStack) -> Int = { it.amount }

    fun count(items: List<ItemStack?>, ingredient: Ingredient): Int =
        StackAllocator.count(items, amountOf, ingredient.matches)

    fun shortages(items: List<ItemStack?>, ingredients: List<Ingredient>): List<Shortage> {
        val allocation = allocate(items, ingredients)
        return ingredients.indices
            .filter { allocation.missing[it] > 0 }
            .map { Shortage(ingredients[it], allocation.taken[it], allocation.missing[it]) }
    }

    fun consume(items: List<ItemStack?>, ingredients: List<Ingredient>): List<ItemStack?>? {
        val allocation = allocate(items, ingredients)
        if (!allocation.complete) return null
        return items.mapIndexed { index, item -> withAmount(item, allocation.remaining[index]) }
    }

    private fun allocate(items: List<ItemStack?>, ingredients: List<Ingredient>): Allocation =
        StackAllocator.allocate(items, amountOf, ingredients.map { Requirement(it.amount, it.matches) })

    private fun withAmount(item: ItemStack?, amount: Int): ItemStack? = when {
        item == null || amount <= 0 -> null
        amount == item.amount -> item
        else -> item.clone().apply { this.amount = amount }
    }
}
