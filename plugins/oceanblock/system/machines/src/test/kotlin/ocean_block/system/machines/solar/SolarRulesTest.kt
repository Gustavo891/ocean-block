package ocean_block.system.machines.solar

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SolarRulesTest : StringSpec({

    "de dia gera 3 E a cada 10 ticks" {
        SolarRules.produce(100, 10, active = true) shouldBe 103
    }

    "parado nao gera" {
        SolarRules.produce(100, 10, active = false) shouldBe 100
    }

    "para no teto de 1000" {
        SolarRules.produce(999, 10, active = true) shouldBe SolarRules.CAPACITY
    }

    "dia vai de 0 a 11999" {
        SolarRules.isDay(0) shouldBe true
        SolarRules.isDay(11999) shouldBe true
        SolarRules.isDay(12000) shouldBe false
        SolarRules.isDay(23999) shouldBe false
        SolarRules.isDay(24500) shouldBe true
    }
})
