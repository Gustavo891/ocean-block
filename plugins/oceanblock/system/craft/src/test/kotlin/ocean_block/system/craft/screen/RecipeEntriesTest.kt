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

    "card bloqueado por pesquisa desliga o botão e manda para o /pesquisa mesmo sem faltar item" {
        val card = RecipeEntries.card(CardInput(gancho, "fishing_rod", 1, emptyList(), lockedBy = "Peneira de ferro"))

        card["falta"] shouldBe "true"
        card["acao"] shouldBe "⌀ Pesquise em /pesquisa: Peneira de ferro"
        card["status"] shouldBe "Requer pesquisa"
        card["status_icone"] shouldBe "ui:lock"
        card["status_estilo"] shouldBe "bad"
    }

    "card mostra cada ingrediente num slot com a quantidade pedida e marca o que falta" {
        val receita = recipe(
            "gancho",
            Ingredient("Tábua", 3, "Tábuas", "oak_planks") { true },
            Ingredient("Linha", 2, "Linhas", "string") { true },
        )
        val shortages = listOf(Shortage(receita.ingredients[1], have = 1, missing = 1))

        val card = RecipeEntries.card(CardInput(receita, "fishing_rod", 1, shortages, owned = listOf(12, 1)))

        card["i0_item"] shouldBe "oak_planks"
        card["i0_qtd"] shouldBe "3"
        card["i0_nome"] shouldBe "Tábuas"
        card["i0_tem"] shouldBe "<green>Você tem 12"
        card["i0_falta"] shouldBe "false"
        card["i1_item"] shouldBe "string"
        card["i1_qtd"] shouldBe "2"
        card["i1_tem"] shouldBe "<red>Você tem 1, falta 1"
        card["i1_falta"] shouldBe "true"
        card["i2_item"] shouldBe ""
        card["i4_item"] shouldBe ""
        card["status"] shouldBe "Faltam itens"
        card["status_icone"] shouldBe "ui:dot-red"
        card["status_estilo"] shouldBe "bad"
    }

    "card diz quanto a receita rende" {
        RecipeEntries.card(CardInput(gancho, "fishing_rod", 1, emptyList()))["rende"] shouldBe "1 unidade"
        RecipeEntries.card(CardInput(gancho, "fishing_rod", 4, emptyList()))["rende"] shouldBe "4 unidades"
    }

    "receita guarda a pesquisa que a libera" {
        recipe("peneira_ferro", tabuas(1)).copy(research = "peneira_ferro").research shouldBe "peneira_ferro"
        gancho.research shouldBe null
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
        card["status"] shouldBe "Pronto para craftar"
        card["status_icone"] shouldBe "ui:check"
        card["status_estilo"] shouldBe "good"
    }

    "card sem material desliga o botão e lista o que falta" {
        val shortages = listOf(Shortage(gancho.ingredients[0], have = 2, missing = 1), Shortage(gancho.ingredients[1], have = 0, missing = 2))

        val card = RecipeEntries.card(CardInput(gancho, "fishing_rod", 1, shortages))

        card["falta"] shouldBe "true"
        card["acao"] shouldBe "⌀ Falta: 1 Tábua ∙ 2 Linhas"
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

    "páginas de 4 cards com a página limitada ao intervalo" {
        RecipeEntries.pageCount(0) shouldBe 1
        RecipeEntries.pageCount(4) shouldBe 1
        RecipeEntries.pageCount(5) shouldBe 2
        RecipeEntries.clampPage(5, 7) shouldBe 1
        RecipeEntries.clampPage(-1, 7) shouldBe 0
        RecipeEntries.clampPage(3, 2) shouldBe 0
    }

    "a linha de páginas some com uma página só" {
        RecipeEntries.pagination(0, 4).shouldBeEmpty()
    }

    "a linha de páginas desliga as setas nas pontas" {
        RecipeEntries.pagination(0, 13) shouldContainExactly listOf(
            mapOf("pagina" to "1", "paginas" to "4", "sem_anterior" to "true", "sem_proxima" to "false"),
        )
        RecipeEntries.pagination(1, 13).single()["sem_anterior"] shouldBe "false"
        RecipeEntries.pagination(3, 13).single()["sem_proxima"] shouldBe "true"
    }

    "a coluna de categorias comporta todas as categorias que existem" {
        RecipeCategory.entries.size shouldBeLessThanOrEqual RecipeEntries.CATEGORIES_PER_PAGE
    }
})
