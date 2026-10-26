package ocean_block.system.custom_items.content.paxel

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PaxelRecipeRulesTest : StringSpec({

    "machado, picareta e pa em cima e dois gravetos no meio" {
        PaxelRecipeRules.SHAPE shouldBe listOf("APS", " T ", " T ")
    }

    "as ferramentas sao do mesmo material do paxel" {
        PaxelRecipeRules.ingredients("IRON_PICKAXE") shouldBe mapOf(
            'A' to "IRON_AXE",
            'P' to "IRON_PICKAXE",
            'S' to "IRON_SHOVEL",
            'T' to "STICK",
        )
        PaxelRecipeRules.ingredients("NETHERITE_PICKAXE").getValue('S') shouldBe "NETHERITE_SHOVEL"
    }

    "item customizado na grade cancela o paxel" {
        PaxelRecipeRules.allowsMatrix(listOf(null, null, null)) shouldBe true
        PaxelRecipeRules.allowsMatrix(listOf(null, "picareta_fundidora", null)) shouldBe false
        PaxelRecipeRules.allowsMatrix(listOf("iron_paxel")) shouldBe false
    }

    "chave da receita por paxel" {
        PaxelRecipeRules.key("diamond_paxel") shouldBe "paxel_diamond_paxel"
    }
})
