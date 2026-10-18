package ocean_block.system.drift.collect

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

class CollectorRuleTest : StringSpec({

    val owner = UUID.randomUUID()
    val members = setOf("alice")
    val trusted = setOf("bob")

    "dono pode coletar" {
        CollectorRule.canCollect(owner, members, trusted, owner, "Zed") shouldBe true
    }

    "membro com nome em caixa diferente pode coletar" {
        CollectorRule.canCollect(owner, members, trusted, UUID.randomUUID(), "ALICE") shouldBe true
    }

    "jogador de confianca pode coletar" {
        CollectorRule.canCollect(owner, members, trusted, UUID.randomUUID(), "Bob") shouldBe true
    }

    "estranho nao pode coletar" {
        CollectorRule.canCollect(owner, members, trusted, UUID.randomUUID(), "Eve") shouldBe false
    }
})
