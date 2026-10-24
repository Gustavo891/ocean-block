package ocean_block.system.machines.energy

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.util.UUID

class EnergyRulesTest : StringSpec({

    val a = UUID(0, 1)
    val b = UUID(0, 2)
    val c = UUID(0, 3)

    "tira da mais perto primeiro" {
        EnergyRules.plan(30, listOf(EnergySource(a, 6.0, 100), EnergySource(b, 2.0, 100))) shouldBe mapOf(b to 30L)
    }

    "soma varias fontes" {
        EnergyRules.plan(50, listOf(EnergySource(a, 1.0, 20), EnergySource(b, 3.0, 100))) shouldBe mapOf(a to 20L, b to 30L)
    }

    "insuficiente devolve null e nao tira nada" {
        EnergyRules.plan(50, listOf(EnergySource(a, 1.0, 20), EnergySource(b, 3.0, 10))).shouldBeNull()
    }

    "fonte fora do raio nao conta" {
        EnergyRules.plan(10, listOf(EnergySource(a, 8.5, 100))).shouldBeNull()
        EnergyRules.plan(10, listOf(EnergySource(a, 8.0, 100))) shouldBe mapOf(a to 10L)
    }

    "fonte vazia e ignorada" {
        EnergyRules.plan(10, listOf(EnergySource(a, 1.0, 0), EnergySource(b, 2.0, 10))) shouldBe mapOf(b to 10L)
    }

    "empate por uuid" {
        EnergyRules.plan(10, listOf(EnergySource(c, 2.0, 100), EnergySource(a, 2.0, 100))) shouldBe mapOf(a to 10L)
    }

    "zero devolve vazio" {
        EnergyRules.plan(0, emptyList()) shouldBe emptyMap()
    }
})
