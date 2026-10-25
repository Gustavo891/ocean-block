package ocean_block.system.craft.recipe

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class RecipeLocksTest : StringSpec({

    "sem provider nada trava" {
        RecipeLocks.required(8, null) shouldBe null
    }

    "jogador abaixo do nivel recebe o nivel exigido" {
        RecipeLocks.required(8, 7) shouldBe 8
    }

    "jogador no nivel ou acima nao trava" {
        RecipeLocks.required(8, 8) shouldBe null
        RecipeLocks.required(1, 1) shouldBe null
    }
})
