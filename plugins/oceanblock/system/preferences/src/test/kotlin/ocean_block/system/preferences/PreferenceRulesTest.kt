package ocean_block.system.preferences

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PreferenceRulesTest : StringSpec({

    "sem valor guardado vale o padrao" {
        PreferenceRules.isEnabled(null, Preference.ENERGY_TRAIL) shouldBe true
    }

    "valor guardado manda" {
        PreferenceRules.isEnabled(0, Preference.ENERGY_TRAIL) shouldBe false
        PreferenceRules.isEnabled(1, Preference.ENERGY_TRAIL) shouldBe true
    }

    "alternar inverte o valor efetivo" {
        PreferenceRules.toggled(null, Preference.ENERGY_TRAIL) shouldBe 0.toByte()
        PreferenceRules.toggled(0, Preference.ENERGY_TRAIL) shouldBe 1.toByte()
    }

    "rastro de energia tem chave e nome" {
        Preference.ENERGY_TRAIL.key shouldBe "rastro_energia"
        Preference.ENERGY_TRAIL.displayName shouldBe "Rastro de energia"
    }

    "chaves sao unicas" {
        Preference.entries.map { it.key }.distinct().size shouldBe Preference.entries.size
    }
})
