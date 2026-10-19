package ocean_block.system.craft.recipe

open class RecipeBook {

    private val recipes = LinkedHashMap<String, OceanRecipe>()

    fun register(recipe: OceanRecipe) {
        require(recipe.id !in recipes) { "receita '${recipe.id}' já registrada" }
        recipes[recipe.id] = recipe
    }

    fun get(id: String): OceanRecipe? = recipes[id]

    fun all(): List<OceanRecipe> = recipes.values.toList()

    fun byCategory(): Map<RecipeCategory, List<OceanRecipe>> {
        val grouped = recipes.values.groupBy { it.category }
        return RecipeCategory.entries
            .mapNotNull { category -> grouped[category]?.let { category to it } }
            .toMap()
    }
}

object Recipes : RecipeBook()
