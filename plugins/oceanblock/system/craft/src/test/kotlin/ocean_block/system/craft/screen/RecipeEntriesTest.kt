package ocean_block.system.craft.screen

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import ocean_block.system.craft.recipe.Ingredient
import ocean_block.system.craft.recipe.OceanRecipe
import ocean_block.system.craft.recipe.RecipeCategory
import ocean_block.system.craft.recipe.Shortage
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

private fun tabuas(amount: Int) = Ingredient("Tábua", amount, "Tábuas") { true }
private fun linhas(amount: Int) = Ingredient("Linha", amount, "Linhas") { true }
private fun ingredient(label: String) = Ingredient(label, 1, "${label}s") { true }

private fun recipe(id: String, vararg ingredients: Ingredient, category: RecipeCategory = RecipeCategory.FERRAMENTAS) = OceanRecipe(
    id = id,
    name = id.replaceFirstChar(Char::uppercase),
    description = "Descrição de $id.",
    category = category,
    result = { ItemStack(Material.STICK) },
    ingredients = ingredients.toList(),
)

class RecipeEntriesTest : StringSpec({

    val gancho = recipe("gancho", tabuas(3), linhas(2))

    "custo junta os ingredientes no plural certo" {
        RecipeEntries.cost(gancho.ingredients) shouldBe "3 Tábuas ∙ 2 Linhas"
        RecipeEntries.cost(listOf(linhas(1))) shouldBe "1 Linha"
    }

    "card pronto traz item, nome, custo, botão ligado e dica de pronto" {
        val card = RecipeEntries.card(CardInput(gancho, "fishing_rod", 1, emptyList()))

        card["item"] shouldBe "fishing_rod"
        card["qtd"] shouldBe "1"
        card["nome"] shouldBe "Gancho"
        card["descricao"] shouldBe "Descrição de gancho."
        card["custo"] shouldBe "3 Tábuas ∙ 2 Linhas"
        card["custo_lista"] shouldBe "∙ 3 Tábuas\n∙ 2 Linhas"
        card["falta"] shouldBe "false"
        card["acao"] shouldBe "✔ Pronto! Clique para craftar."
    }

    "card sem material desliga o botão e lista o que falta" {
        val shortages = listOf(Shortage(gancho.ingredients[0], have = 2, missing = 1), Shortage(gancho.ingredients[1], have = 0, missing = 2))

        val card = RecipeEntries.card(CardInput(gancho, "fishing_rod", 1, shortages))

        card["falta"] shouldBe "true"
        card["acao"] shouldBe "⌀ Falta: 1 Tábua ∙ 2 Linhas"
    }

    "chips usam os ingredientes da categoria sem repetir rótulo, no máximo 4, com a contagem do jogador" {
        val rede = recipe("rede", tabuas(4), linhas(5))
        val outra = recipe("outra", ingredient("Pena"), ingredient("Osso"), ingredient("Pedra"))

        val ingredients = RecipeEntries.chipIngredients(listOf(gancho, rede, outra))
        val chips = RecipeEntries.chips(ingredients, mapOf("Tábua" to 12, "Linha" to 1))

        ingredients.map { it.label } shouldContainExactly listOf("Tábua", "Linha", "Pena", "Osso")
        chips.map { it["texto"] } shouldContainExactly listOf("12 Tábuas", "1 Linha", "0 Penas", "0 Ossos")
    }

    "categorias marcam a aberta e trazem nome, ícone e dica" {
        val entries = RecipeEntries.categories(listOf(RecipeCategory.FERRAMENTAS, RecipeCategory.COLETA), RecipeCategory.COLETA)

        entries.map { it["nome"] } shouldContainExactly listOf("Ferramentas", "Coleta")
        entries.map { it["icone"] } shouldContainExactly listOf("ui:pickaxe", "ui:chest")
        entries.map { it["ativa"] } shouldContainExactly listOf("false", "true")
        entries.map { it["acao"] } shouldContainExactly listOf("Clique para ver!", "✔ Aberta agora!")
        entries[0]["descricao"] shouldBe RecipeCategory.FERRAMENTAS.description
    }

    "categoria guardada vale enquanto existir; senão abre a primeira" {
        val available = listOf(RecipeCategory.FERRAMENTAS, RecipeCategory.COLETA)

        RecipeEntries.activeCategory(RecipeCategory.COLETA, available) shouldBe RecipeCategory.COLETA
        RecipeEntries.activeCategory(null, available) shouldBe RecipeCategory.FERRAMENTAS
        RecipeEntries.activeCategory(RecipeCategory.COLETA, listOf(RecipeCategory.FERRAMENTAS)) shouldBe RecipeCategory.FERRAMENTAS
        RecipeEntries.activeCategory(null, emptyList()) shouldBe null
    }

    "páginas de 6 cards com a página limitada ao intervalo" {
        RecipeEntries.pageCount(0) shouldBe 1
        RecipeEntries.pageCount(6) shouldBe 1
        RecipeEntries.pageCount(7) shouldBe 2
        RecipeEntries.clampPage(5, 7) shouldBe 1
        RecipeEntries.clampPage(-1, 7) shouldBe 0
        RecipeEntries.clampPage(3, 2) shouldBe 0
    }

    "a linha de páginas some com uma página só" {
        RecipeEntries.pagination(0, 6).shouldBeEmpty()
    }

    "a linha de páginas desliga as setas nas pontas" {
        RecipeEntries.pagination(0, 13) shouldContainExactly listOf(
            mapOf("pagina" to "1", "paginas" to "3", "sem_anterior" to "true", "sem_proxima" to "false"),
        )
        RecipeEntries.pagination(1, 13).single()["sem_anterior"] shouldBe "false"
        RecipeEntries.pagination(2, 13).single()["sem_proxima"] shouldBe "true"
    }

    "a coluna de categorias comporta todas as categorias que existem" {
        RecipeCategory.entries.size shouldBeLessThanOrEqual RecipeEntries.CATEGORIES_PER_PAGE
    }
})
