package ocean_block.system.level.dialog

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import ocean_block.system.craft.recipe.Ingredient
import ocean_block.system.craft.recipe.OceanRecipe
import ocean_block.system.craft.recipe.RecipeCategory
import ocean_block.system.level.curve.LevelCurve
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

private fun recipe(id: String, name: String, level: Int) = OceanRecipe(
    id = id,
    name = name,
    description = "",
    category = RecipeCategory.FERRAMENTAS,
    result = { ItemStack(Material.STONE) },
    ingredients = listOf(Ingredient("Pedregulho", 1) { true }),
    level = level,
)

private val RECIPES = listOf(
    recipe("gancho", "Gancho", 1),
    recipe("rede", "Rede de Detritos", 3),
    recipe("picareta_fundidora", "Picareta fundidora", 12),
    recipe("sem_icone", "Coisa nova", 14),
).groupBy { it.level }

private fun xpAt(level: Int, extra: Long = 0) = LevelCurve.totalFor(level) + extra

class LevelScreenLoreTest : StringSpec({

    "duas paginas de 25 niveis em serpente" {
        LevelScreenLore.pages() shouldBe 2
        LevelScreenLore.order(0)[0] shouldContainExactly listOf(1, 2, 3, 4, 5)
        LevelScreenLore.order(0)[1] shouldContainExactly listOf(10, 9, 8, 7, 6)
        LevelScreenLore.order(1)[4] shouldContainExactly listOf(46, 47, 48, 49, 50)
        LevelScreenLore.pageOf(25) shouldBe 0
        LevelScreenLore.pageOf(26) shouldBe 1
    }

    "no feito ganha selo, o atual borda e o futuro fica apagado" {
        val row = LevelScreenLore.row(0, 1, xpAt(9), RECIPES)

        row.map { it["numero"] } shouldContainExactly listOf("10", "9", "8", "7", "6")
        row[0]["estado"] shouldBe "ui:no_trancada"
        row[1]["estado"] shouldBe "ui:no_atual"
        row[2]["estado"] shouldBe ""
        row[2]["selo"] shouldBe "ui:no_feita"
        row[1]["selo"] shouldBe ""
    }

    "ligacao fica verde so ate o nivel atual" {
        val row = LevelScreenLore.row(0, 1, xpAt(9), RECIPES)

        row.map { it["liga"] } shouldContainExactly listOf("ui:liga_cinza", "ui:liga_verde", "ui:liga_verde", "ui:liga_verde", "")
        LevelScreenLore.drop(0, 0, 9)["liga"] shouldBe "ui:desce_verde"
        LevelScreenLore.drop(0, 1, 9)["liga"] shouldBe "ui:desce_cinza"
    }

    "nivel com receita mostra o icone e o numero no canto" {
        val row = LevelScreenLore.row(0, 0, xpAt(9), RECIPES)

        row[0]["icone"] shouldBe "item:fishing_rod"
        row[0]["numero"] shouldBe ""
        row[0]["canto"] shouldBe "1"
        row[1]["icone"] shouldBe ""
        row[1]["canto"] shouldBe ""
        row[2]["dica"] shouldBe "Nível 3\n∙ Rede de Detritos\n\n✔ Alcançado"
    }

    "receita sem icone conhecido volta a mostrar o numero" {
        val node = LevelScreenLore.row(0, 2, xpAt(9), RECIPES)[3]

        node["numero"] shouldBe "14"
        node["icone"] shouldBe ""
    }

    "dica do nivel trancado diz quanto XP falta" {
        val node = LevelScreenLore.row(0, 2, xpAt(9), RECIPES)[1]

        node["dica"] shouldBe "Nível 12\n∙ Picareta fundidora\n\n✖ Faltam ${LevelScreenLore.number(xpAt(12) - xpAt(9))} XP"
        LevelScreenLore.row(0, 1, xpAt(9), RECIPES)[1]["dica"] shouldBe "Nível 9\n\nVocê está aqui"
    }

    "cabecalho do jogador mostra nivel, barra e quanto falta" {
        val header = LevelScreenLore.header(xpAt(9, 100))

        header["nivel"] shouldBe "9"
        header["nivel_de"] shouldBe "NÍVEL 9 DE 50"
        header["xp"] shouldBe "100/${LevelScreenLore.number(LevelCurve.toNext(9))}"
        header["barra"] shouldBe (100 * 100 / LevelCurve.toNext(9)).toString()
        header["faltam"] shouldBe "FALTAM ${LevelScreenLore.number(LevelCurve.toNext(9) - 100)} XP PARA O NÍVEL 10"
    }

    "no nivel maximo a barra enche" {
        val header = LevelScreenLore.header(xpAt(LevelCurve.MAX_LEVEL))

        header["barra"] shouldBe "100"
        header["faltam"] shouldBe "VOCÊ CHEGOU AO TOPO"
    }

    "proximo nivel traz XP que falta e coins" {
        val next = LevelScreenLore.next(xpAt(9, 100), RECIPES)

        next.single()["numero"] shouldBe "10"
        next.single()["titulo"] shouldBe "NÍVEL 10"
        next.single()["faltam"] shouldBe "FALTAM ${LevelScreenLore.number(LevelCurve.toNext(9) - 100)} XP"
        next.single()["coins"] shouldContain "+"
    }

    "proxima receita aponta o primeiro nivel acima do atual" {
        val recipe = LevelScreenLore.nextRecipe(9, RECIPES).single()

        recipe["icone"] shouldBe "item:iron_pickaxe"
        recipe["nivel"] shouldBe "NÍVEL 12"
        recipe["nome"] shouldBe "Picareta fundidora"
    }

    "sem proximo nivel nem receita as listas ficam vazias" {
        LevelScreenLore.next(xpAt(LevelCurve.MAX_LEVEL), RECIPES).shouldBeEmpty()
        LevelScreenLore.nextRecipe(14, RECIPES).shouldBeEmpty()
    }

    "paginador destaca a pagina aberta" {
        val pager = LevelScreenLore.pager(1)

        pager["faixa"] shouldBe "NÍVEIS 26 A 50"
        pager["pagina1_estilo"] shouldBe "muted"
        pager["pagina2_estilo"] shouldBe "branco"
    }
})
