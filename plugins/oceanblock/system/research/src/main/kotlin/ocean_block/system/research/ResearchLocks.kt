package ocean_block.system.research

import ocean_block.system.craft.recipe.OceanRecipe
import ocean_block.system.research.tree.ResearchTree

object ResearchLocks {

    fun reason(recipe: OceanRecipe, completed: Set<String>?): String? {
        val research = recipe.research ?: return null
        if (completed != null && research in completed) return null
        return ResearchTree.byId[research]?.name ?: research
    }
}
