package ocean_block.system.islands.choice

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import ocean_block.system.islands.island.type.IslandType

class ChoiceRulesTest : StringSpec({
    "so quem nao tem ilha nem participa escolhe" {
        ChoiceRules.needsChoice(ownsIsland = false, memberSomewhere = false) shouldBe true
        ChoiceRules.needsChoice(ownsIsland = true, memberSomewhere = false) shouldBe false
        ChoiceRules.needsChoice(ownsIsland = false, memberSomewhere = true) shouldBe false
        ChoiceRules.needsChoice(ownsIsland = true, memberSomewhere = true) shouldBe false
    }

    "login sem casa escolhe, com casa e marca restaura" {
        ChoiceRules.afterLogin(marked = true, hasHome = false) shouldBe ChoiceRules.LoginAction.CHOOSE
        ChoiceRules.afterLogin(marked = false, hasHome = false) shouldBe ChoiceRules.LoginAction.CHOOSE
        ChoiceRules.afterLogin(marked = true, hasHome = true) shouldBe ChoiceRules.LoginAction.RESTORE
        ChoiceRules.afterLogin(marked = false, hasHome = true) shouldBe ChoiceRules.LoginAction.NOTHING
    }

    "texto da contagem" {
        ChoiceRules.countText(0) shouldBe "nenhuma ilha"
        ChoiceRules.countText(1) shouldBe "1 ilha"
        ChoiceRules.countText(12) shouldBe "12 ilhas"
    }

    "uma linha por tipo na ordem da tela" {
        val rows = ChoiceRules.rows(mapOf(IslandType.DESERTO to 3, IslandType.JANGADA to 1))
        rows.map { it["id"] } shouldBe listOf("padrao", "deserto", "gelo", "jangada")
        rows[0] shouldBe mapOf(
            "id" to "padrao",
            "nome" to "PADRÃO",
            "descricao" to "A ilha de sempre: grama, árvore e lava.",
            "contagem" to "nenhuma ilha",
            "icone" to "ui:ilha_tipo/padrao",
        )
        rows[1]["contagem"] shouldBe "3 ilhas"
        rows[3]["contagem"] shouldBe "1 ilha"
        rows[3]["nome"] shouldBe "JANGADA"
    }

    "sem contagens todas dizem nenhuma ilha" {
        ChoiceRules.rows(emptyMap()).map { it["contagem"] } shouldBe List(4) { "nenhuma ilha" }
    }
})
