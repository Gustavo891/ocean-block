package one_block.textura.block

import one_block.textura.registry.BlockRegistry
import org.bukkit.Bukkit
import org.bukkit.Keyed
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.CookingRecipe
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.inventory.SmithingRecipe
import org.bukkit.inventory.SmithingTransformRecipe
import org.bukkit.inventory.SmithingTrimRecipe
import org.bukkit.inventory.StonecuttingRecipe
import java.util.logging.Logger

object VanillaRecipeRemover {

    private const val VANILLA_NAMESPACE = "minecraft"

    private val removidas = mutableSetOf<NamespacedKey>()

    fun apply(registry: BlockRegistry, logger: Logger) {
        val alvos = registry.overridesByTarget.keys
            .mapNotNull { Material.matchMaterial(it) }
            .toSet()
        if (alvos.isEmpty()) return

        val chaves = mutableListOf<NamespacedKey>()
        val iterator = Bukkit.recipeIterator()
        while (iterator.hasNext()) {
            val recipe = runCatching { iterator.next() }.getOrNull() ?: continue
            val chave = (recipe as? Keyed)?.key ?: continue
            if (chave.namespace != VANILLA_NAMESPACE) continue
            if (chave in removidas) continue

            val produz = runCatching { recipe.result.type in alvos }.getOrDefault(false)
            val consome = runCatching { usaAlgum(recipe, alvos) }.getOrDefault(false)
            if (!produz && !consome) continue

            chaves += chave
        }

        if (chaves.isEmpty()) return

        chaves.forEach { chave ->
            if (Bukkit.removeRecipe(chave)) removidas += chave
        }

        logger.info(
            "${chaves.size} receita(s) vanilla removida(s) por override de bloco: " +
                chaves.joinToString(", ") { it.toString() }
        )
    }

    private fun usaAlgum(recipe: Recipe, alvos: Set<Material>): Boolean =
        ingredientes(recipe).any { contem(it, alvos) }

    private fun ingredientes(recipe: Recipe): List<RecipeChoice> = when (recipe) {
        is ShapedRecipe -> recipe.choiceMap.values.filterNotNull()
        is ShapelessRecipe -> recipe.choiceList
        is CookingRecipe<*> -> listOf(recipe.inputChoice)
        is StonecuttingRecipe -> listOf(recipe.inputChoice)
        is SmithingTransformRecipe -> listOf(recipe.template, recipe.base, recipe.addition)
        is SmithingTrimRecipe -> listOf(recipe.template, recipe.base, recipe.addition)
        is SmithingRecipe -> listOf(recipe.base, recipe.addition)
        else -> emptyList()
    }

    private fun contem(choice: RecipeChoice, alvos: Set<Material>): Boolean = when (choice) {
        is RecipeChoice.MaterialChoice -> choice.choices.any { it in alvos }
        is RecipeChoice.ExactChoice -> choice.choices.any { it.type in alvos }
        else -> false
    }
}
