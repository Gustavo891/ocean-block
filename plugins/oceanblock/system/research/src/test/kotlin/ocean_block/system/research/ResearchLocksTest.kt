package ocean_block.system.research

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ocean_block.system.craft.recipe.Ingredient
import ocean_block.system.craft.recipe.OceanRecipe
import ocean_block.system.craft.recipe.RecipeCategory
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

private fun recipe(research: String?) = OceanRecipe(
    id = "rede",
    name = "Rede",
    description = "Rede.",
    category = RecipeCategory.COLETA,
    result = { ItemStack(Material.LOOM) },
    ingredients = listOf(Ingredient("Tábua", 1) { true }),
    research = research,
)

class ResearchLocksTest : StringSpec({

    "receita sem pesquisa nunca bloqueia" {
        ResearchLocks.reason(recipe(null), emptySet()).shouldBeNull()
        ResearchLocks.reason(recipe(null), null).shouldBeNull()
    }

    "pesquisa concluida libera" {
        ResearchLocks.reason(recipe("rede"), setOf("rede")).shouldBeNull()
    }

    "pesquisa pendente bloqueia com o nome da pesquisa" {
        ResearchLocks.reason(recipe("rede"), emptySet()) shouldBe "Rede de Detritos"
    }

    "sem raft fica bloqueada" {
        ResearchLocks.reason(recipe("rede"), null) shouldBe "Rede de Detritos"
    }

    "pesquisa fora da arvore bloqueia pelo id" {
        ResearchLocks.reason(recipe("desconhecida"), emptySet()) shouldBe "desconhecida"
    }
})
