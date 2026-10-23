package ocean_block.system.craft.menu

import ocean_block.system.craft.recipe.Ingredient
import ocean_block.system.craft.recipe.OceanRecipe
import ocean_block.system.craft.recipe.RecipeCategory
import ocean_block.system.craft.recipe.Shortage

data class RecipeCard(
    val recipe: OceanRecipe,
    val amount: Int,
    val shortages: List<Shortage>,
    val lockedBy: String? = null,
    val owned: List<Int> = emptyList(),
)

object RecipeLore {

    const val CATEGORY_SLOTS = 3
    private const val SEPARATOR = " ∙ "

    fun name(card: RecipeCard): String {
        val base = "<white>${card.recipe.name}"
        return if (card.amount > 1) "$base <gray>x${card.amount}" else base
    }

    fun lore(card: RecipeCard): List<String> {
        val ingredients = card.recipe.ingredients.mapIndexed { index, ingredient ->
            val have = card.owned.getOrNull(index) ?: 0
            val short = card.shortages.any { it.ingredient === ingredient }
            val mark = if (short) "<red>✖" else "<green>✔"
            "$mark <gray>${amountOf(ingredient, ingredient.amount)} <dark_gray>(você tem $have)"
        }
        return listOf("<gray>${card.recipe.description}", "", "<white>Ingredientes:") + ingredients + listOf("", status(card))
    }

    fun categoryName(category: RecipeCategory, active: Boolean): String =
        (if (active) "<green>" else "<white>") + category.displayName

    fun categoryLore(category: RecipeCategory, active: Boolean): List<String> =
        listOf("<gray>${category.description}", "", if (active) "<green>Aberta agora." else "<yellow>Clique para ver!")

    fun activeCategory(wanted: RecipeCategory?, available: List<RecipeCategory>): RecipeCategory? =
        wanted?.takeIf { it in available } ?: available.firstOrNull()

    fun missingText(shortages: List<Shortage>): String =
        "Falta: " + shortages.joinToString(SEPARATOR) { amountOf(it.ingredient, it.missing) }

    private fun status(card: RecipeCard): String = when {
        card.lockedBy != null -> "<red>Pesquise em <white>/pesquisa</white>: ${card.lockedBy}"
        card.shortages.isNotEmpty() -> "<red>Faltam itens."
        else -> "<yellow>Clique para craftar!"
    }

    private fun amountOf(ingredient: Ingredient, count: Int): String = "$count ${ingredient.labelFor(count)}"
}
