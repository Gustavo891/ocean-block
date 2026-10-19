package ocean_block.system.craft.recipe

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

private val linha = Ingredient(label = "Linha", amount = 1) { it.type == Material.STRING }

private fun recipe(id: String, category: RecipeCategory, name: String = id) = OceanRecipe(
    id = id,
    name = name,
    description = "",
    category = category,
    result = { ItemStack(Material.STICK) },
    ingredients = listOf(linha),
)

class RecipeBookTest : StringSpec({

    "registra e busca pelo id" {
        val book = RecipeBook()
        val gancho = recipe("gancho", RecipeCategory.FERRAMENTAS)

        book.register(gancho)

        book.get("gancho") shouldBe gancho
        book.get("rede").shouldBeNull()
    }

    "agrupa por categoria na ordem do enum e do registro, sem categorias vazias" {
        val book = RecipeBook()
        val rede = recipe("rede", RecipeCategory.COLETA)
        val gancho = recipe("gancho", RecipeCategory.FERRAMENTAS)
        val remo = recipe("remo", RecipeCategory.FERRAMENTAS)

        book.register(rede)
        book.register(gancho)
        book.register(remo)

        book.byCategory() shouldContainExactly mapOf(
            RecipeCategory.FERRAMENTAS to listOf(gancho, remo),
            RecipeCategory.COLETA to listOf(rede),
        )
        book.byCategory().keys.toList() shouldContainExactly listOf(RecipeCategory.FERRAMENTAS, RecipeCategory.COLETA)
    }

    "so categorias com receita aparecem" {
        val book = RecipeBook()
        book.register(recipe("rede", RecipeCategory.COLETA))

        book.byCategory().keys.toList() shouldContainExactly listOf(RecipeCategory.COLETA)
    }

    "id duplicado e rejeitado e mantem a primeira receita" {
        val book = RecipeBook()
        val primeira = recipe("gancho", RecipeCategory.FERRAMENTAS, name = "Gancho")

        book.register(primeira)

        shouldThrow<IllegalArgumentException> {
            book.register(recipe("gancho", RecipeCategory.COLETA, name = "Outro"))
        }
        book.get("gancho") shouldBe primeira
        book.all() shouldContainExactly listOf(primeira)
    }

    "receita sem ingredientes e rejeitada" {
        shouldThrow<IllegalArgumentException> {
            OceanRecipe("vazia", "Vazia", "", RecipeCategory.COLETA, { ItemStack(Material.STICK) }, emptyList())
        }
    }

    "ouvinte recebe as receitas já registradas e as próximas, uma vez cada" {
        val book = RecipeBook()
        val gancho = recipe("gancho", RecipeCategory.FERRAMENTAS)
        val rede = recipe("rede", RecipeCategory.COLETA)
        val vistas = mutableListOf<String>()
        book.register(gancho)

        book.onRegister { vistas += it.id }
        book.register(rede)

        vistas shouldContainExactly listOf("gancho", "rede")
    }

    "id duplicado nao chega ao ouvinte" {
        val book = RecipeBook()
        val vistas = mutableListOf<String>()
        book.onRegister { vistas += it.id }
        book.register(recipe("gancho", RecipeCategory.FERRAMENTAS))

        shouldThrow<IllegalArgumentException> { book.register(recipe("gancho", RecipeCategory.COLETA)) }

        vistas shouldContainExactly listOf("gancho")
    }
})
