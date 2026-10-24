package ocean_block.system.craft.menu

import io.kotest.core.spec.style.StringSpec
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

private fun recipe(id: String, vararg ingredients: Ingredient) = OceanRecipe(
    id = id,
    name = id.replaceFirstChar(Char::uppercase),
    description = "Descrição de $id.",
    category = RecipeCategory.FERRAMENTAS,
    result = { ItemStack(Material.STICK) },
    ingredients = ingredients.toList(),
)

class RecipeLoreTest : StringSpec({

    val gancho = recipe("gancho", tabuas(3), linhas(2))

    "nome mostra a quantidade so quando rende mais de um" {
        RecipeLore.name(RecipeCard(gancho, 1, emptyList())) shouldBe "<white>Gancho"
        RecipeLore.name(RecipeCard(gancho, 4, emptyList())) shouldBe "<white>Gancho <gray>x4"
    }

    "receita pronta lista os ingredientes em verde e chama para craftar" {
        val lore = RecipeLore.lore(RecipeCard(gancho, 1, emptyList(), owned = listOf(12, 2)))

        lore shouldContainExactly listOf(
            "<gray>Descrição de gancho.",
            "",
            "<white>Ingredientes:",
            "<green>✔ <gray>3 Tábuas <dark_gray>(você tem 12)",
            "<green>✔ <gray>2 Linhas <dark_gray>(você tem 2)",
            "",
            "<yellow>Clique para craftar!",
        )
    }

    "ingrediente que falta fica vermelho e a receita diz que faltam itens" {
        val shortages = listOf(Shortage(gancho.ingredients[1], have = 1, missing = 1))

        val lore = RecipeLore.lore(RecipeCard(gancho, 1, shortages, owned = listOf(3, 1)))

        lore[4] shouldBe "<red>✖ <gray>2 Linhas <dark_gray>(você tem 1)"
        lore.last() shouldBe "<red>Faltam itens."
    }

    "receita bloqueada manda para o /pesquisa" {
        RecipeLore.lore(RecipeCard(gancho, 1, emptyList(), lockedBy = "Peneira de ferro")).last() shouldBe
            "<red>Pesquise em <white>/pesquisa</white>: Peneira de ferro"
    }

    "categoria aberta e fechada" {
        RecipeLore.categoryName(RecipeCategory.COLETA, active = true) shouldBe "<green>Coleta"
        RecipeLore.categoryName(RecipeCategory.COLETA, active = false) shouldBe "<white>Coleta"
        RecipeLore.categoryLore(RecipeCategory.COLETA, active = true).last() shouldBe "<green>Aberta agora."
        RecipeLore.categoryLore(RecipeCategory.COLETA, active = false).last() shouldBe "<yellow>Clique para ver!"
    }

    "categoria guardada vale enquanto existir; senao abre a primeira" {
        val available = listOf(RecipeCategory.FERRAMENTAS, RecipeCategory.COLETA)

        RecipeLore.activeCategory(RecipeCategory.COLETA, available) shouldBe RecipeCategory.COLETA
        RecipeLore.activeCategory(null, available) shouldBe RecipeCategory.FERRAMENTAS
        RecipeLore.activeCategory(RecipeCategory.COLETA, listOf(RecipeCategory.FERRAMENTAS)) shouldBe RecipeCategory.FERRAMENTAS
    }

    "falta junta o que falta no plural certo" {
        val shortages = listOf(Shortage(gancho.ingredients[0], have = 2, missing = 1), Shortage(gancho.ingredients[1], have = 0, missing = 2))

        RecipeLore.missingText(shortages) shouldBe "Falta: 1 Tábua ∙ 2 Linhas"
    }

    "a coluna do menu comporta todas as categorias" {
        RecipeCategory.entries.size shouldBeLessThanOrEqual RecipeLore.CATEGORY_SLOTS
    }

    "a quarta vaga e a categoria Maquinas" {
        RecipeLore.CATEGORY_SLOTS shouldBe 4
        RecipeCategory.entries.last() shouldBe RecipeCategory.MAQUINAS
        RecipeCategory.MAQUINAS.displayName shouldBe "Máquinas"
    }

    "receita guarda a pesquisa que a libera" {
        recipe("peneira_ferro", tabuas(1)).copy(research = "peneira_ferro").research shouldBe "peneira_ferro"
        gancho.research shouldBe null
    }
})
