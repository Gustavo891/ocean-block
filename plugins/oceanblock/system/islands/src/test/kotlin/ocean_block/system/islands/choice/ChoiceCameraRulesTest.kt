package ocean_block.system.islands.choice

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.floats.shouldBeExactly
import io.kotest.matchers.shouldBe

class ChoiceCameraRulesTest : StringSpec({
    "camera fica logo acima do mar" {
        ChoiceRules.CAMERA_Y shouldBeGreaterThan ChoiceRules.SEA_Y.toDouble()
        ChoiceRules.CAMERA_Y + ChoiceRules.EYE_HEIGHT shouldBeLessThan ChoiceRules.SEA_Y + 4.0
    }

    "camera olha para o horizonte" {
        ChoiceRules.CAMERA_PITCH shouldBeExactly 0f
    }

    "chat limpo empurra todas as linhas visiveis" {
        ChoiceRules.CHAT_CLEAR_LINES shouldBe 100
    }
})
