package ocean_block.system.craft.recipe

import java.util.concurrent.CopyOnWriteArrayList

open class RecipeBook {

    private val recipes = LinkedHashMap<String, OceanRecipe>()
    private val listeners = CopyOnWriteArrayList<(OceanRecipe) -> Unit>()

    fun register(recipe: OceanRecipe) {
        require(recipe.id !in recipes) { "receita '${recipe.id}' já registrada" }
        recipes[recipe.id] = recipe
        listeners.forEach { it(recipe) }
    }

    fun onRegister(listener: (OceanRecipe) -> Unit) {
        listeners += listener
        recipes.values.toList().forEach(listener)
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
