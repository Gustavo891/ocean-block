package ocean_block.system.craft.screen

import ocean_block.system.craft.recipe.Ingredient
import ocean_block.system.craft.recipe.OceanRecipe
import ocean_block.system.craft.recipe.RecipeCategory
import ocean_block.system.craft.recipe.Shortage

data class CardInput(
    val recipe: OceanRecipe,
    val item: String,
    val amount: Int,
    val shortages: List<Shortage>,
    val lockedBy: String? = null,
)

object RecipeEntries {

    const val CARDS_PER_PAGE = 6
    const val CATEGORIES_PER_PAGE = 6
    private const val SEPARATOR = " ∙ "
    private const val BULLET = "∙ "
    private const val READY = "✔ Pronto! Clique para craftar."
    private const val MISSING = "⌀ Falta: "
    private const val LOCKED = "⌀ Pesquise na Mesa: "
    private const val OPEN = "✔ Aberta agora!"
    private const val BROWSE = "Clique para ver!"

    fun categories(available: List<RecipeCategory>, active: RecipeCategory): List<Map<String, String>> =
        available.map { category ->
            mapOf(
                "nome" to category.displayName,
                "icone" to category.icon,
                "descricao" to category.description,
                "ativa" to (category == active).toString(),
                "acao" to if (category == active) OPEN else BROWSE,
            )
        }

    fun activeCategory(wanted: RecipeCategory?, available: List<RecipeCategory>): RecipeCategory? =
        wanted?.takeIf { it in available } ?: available.firstOrNull()

    fun card(input: CardInput): Map<String, String> {
        val recipe = input.recipe
        val locked = input.lockedBy
        val missing = locked != null || input.shortages.isNotEmpty()
        return mapOf(
            "item" to input.item,
            "qtd" to input.amount.toString(),
            "nome" to recipe.name,
            "descricao" to recipe.description,
            "custo" to cost(recipe.ingredients),
            "custo_lista" to recipe.ingredients.joinToString("\n") { BULLET + amountOf(it, it.amount) },
            "falta" to missing.toString(),
            "acao" to when {
                locked != null -> lockedText(locked)
                missing -> missingText(input.shortages)
                else -> READY
            },
        )
    }

    fun cost(ingredients: List<Ingredient>): String =
        ingredients.joinToString(SEPARATOR) { amountOf(it, it.amount) }

    fun lockedText(research: String): String = LOCKED + research

    fun missingText(shortages: List<Shortage>): String =
        MISSING + shortages.joinToString(SEPARATOR) { amountOf(it.ingredient, it.missing) }

    fun pageCount(size: Int): Int = maxOf(1, (size + CARDS_PER_PAGE - 1) / CARDS_PER_PAGE)

    fun clampPage(page: Int, size: Int): Int = page.coerceIn(0, pageCount(size) - 1)

    fun pagination(page: Int, size: Int): List<Map<String, String>> {
        val pages = pageCount(size)
        if (pages <= 1) return emptyList()
        val current = clampPage(page, size)
        return listOf(
            mapOf(
                "pagina" to (current + 1).toString(),
                "paginas" to pages.toString(),
                "sem_anterior" to (current == 0).toString(),
                "sem_proxima" to (current == pages - 1).toString(),
            ),
        )
    }

    private fun amountOf(ingredient: Ingredient, count: Int): String = "$count ${ingredient.labelFor(count)}"
}
