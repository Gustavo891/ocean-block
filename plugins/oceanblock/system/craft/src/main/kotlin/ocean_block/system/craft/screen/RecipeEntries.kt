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
    val owned: List<Int> = emptyList(),
)

object RecipeEntries {

    const val CARDS_PER_PAGE = 4
    const val CATEGORIES_PER_PAGE = 6
    private const val SEPARATOR = " ∙ "
    private const val BULLET = "∙ "
    private const val READY = "✔ Pronto! Clique para craftar."
    private const val MISSING = "⌀ Falta: "
    const val INGREDIENT_SLOTS = 5
    private const val LOCKED = "⌀ Pesquise em /pesquisa: "
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
            "rende" to if (input.amount == 1) "1 unidade" else "${input.amount} unidades",
            "acao" to when {
                locked != null -> lockedText(locked)
                missing -> missingText(input.shortages)
                else -> READY
            },
        ) + status(locked != null, input.shortages.isNotEmpty()) + ingredientSlots(input)
    }

    private fun status(locked: Boolean, short: Boolean): Map<String, String> {
        val (text, icon, style) = when {
            locked -> Triple("Requer pesquisa", "ui:lock", "bad")
            short -> Triple("Faltam itens", "ui:dot-red", "bad")
            else -> Triple("Pronto para craftar", "ui:check", "good")
        }
        return mapOf("status" to text, "status_icone" to icon, "status_estilo" to style)
    }

    private fun ingredientSlots(input: CardInput): Map<String, String> =
        (0 until INGREDIENT_SLOTS).flatMap { index ->
            val ingredient = input.recipe.ingredients.getOrNull(index)
            val have = input.owned.getOrNull(index) ?: 0
            val short = input.shortages.firstOrNull { it.ingredient === ingredient }
            listOf(
                "i${index}_item" to (ingredient?.icon.orEmpty()),
                "i${index}_qtd" to (ingredient?.amount?.toString().orEmpty()),
                "i${index}_nome" to (ingredient?.let { it.labelFor(it.amount) }.orEmpty()),
                "i${index}_tem" to when {
                    ingredient == null -> ""
                    short != null -> "<red>Você tem $have, falta ${short.missing}"
                    else -> "<green>Você tem $have"
                },
                "i${index}_falta" to (short != null).toString(),
            )
        }.toMap()

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
