package ocean_block.system.craft.menu

import ocean_block.system.craft.recipe.RecipeCategory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object RecipeSelections {

    private val categories = ConcurrentHashMap<UUID, RecipeCategory>()

    fun get(id: UUID): RecipeCategory? = categories[id]

    fun set(id: UUID, category: RecipeCategory) {
        categories[id] = category
    }

    fun forget(id: UUID) {
        categories.remove(id)
    }
}
