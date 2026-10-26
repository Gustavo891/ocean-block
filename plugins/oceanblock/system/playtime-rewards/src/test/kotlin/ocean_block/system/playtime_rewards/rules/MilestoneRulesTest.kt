package ocean_block.system.playtime_rewards.rules

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import ocean_block.system.playtime_rewards.model.Milestone

private fun milestone(id: String, minutes: Int) = Milestone(id, minutes, coins = 0, pearls = 0, xp = 0)

private val ladder = listOf(milestone("30m", 30), milestone("1h", 60), milestone("2h", 120))

class MilestoneRulesTest : StringSpec({

    "marco abaixo do tempo jogado e não resgatado está disponível" {
        MilestoneRules.status(milestone("1h", 60), 3600, emptySet()) shouldBe MilestoneStatus.Available
    }

    "marco resgatado fica resgatado mesmo com tempo de sobra" {
        MilestoneRules.status(milestone("1h", 60), 99_999, setOf("1h")) shouldBe MilestoneStatus.Claimed
    }

    "marco acima do tempo jogado está bloqueado com os segundos que faltam" {
        MilestoneRules.status(milestone("1h", 60), 3000, emptySet()) shouldBe MilestoneStatus.Locked(600)
    }

    "recém-atingidos são os marcos entre a leitura anterior e a atual" {
        MilestoneRules.reached(ladder, 1700, 3700, emptySet()).map { it.id } shouldContainExactly listOf("30m", "1h")
    }

    "o limite anterior não conta de novo e o atual conta" {
        MilestoneRules.reached(ladder, 1800, 3600, emptySet()).map { it.id } shouldContainExactly listOf("1h")
    }

    "recém-atingido já resgatado não avisa" {
        MilestoneRules.reached(ladder, 0, 7200, setOf("1h")).map { it.id } shouldContainExactly listOf("30m", "2h")
    }

    "tempo que não andou não gera avisos" {
        MilestoneRules.reached(ladder, 4000, 4000, emptySet()).shouldBeEmpty()
    }

    "disponíveis seguem a ordem da escada" {
        MilestoneRules.available(ladder, 7200, setOf("30m")).map { it.id } shouldContainExactly listOf("1h", "2h")
    }

    "resgatar um disponível devolve o novo conjunto" {
        MilestoneRules.claim(milestone("1h", 60), 3600, setOf("30m")) shouldBe ClaimResult.Claimed(setOf("30m", "1h"))
    }

    "resgatar duas vezes não paga" {
        MilestoneRules.claim(milestone("1h", 60), 3600, setOf("1h")) shouldBe ClaimResult.AlreadyClaimed
    }

    "resgatar antes do tempo não paga" {
        MilestoneRules.claim(milestone("1h", 60), 3599, emptySet()) shouldBe ClaimResult.NotReached(1)
    }

    "a página inicial é a do primeiro marco não resgatado" {
        val longLadder = (1..12).map { milestone("m$it", it * 10) }
        val claimed = (1..6).map { "m$it" }.toSet()
        MilestoneRules.startPage(longLadder, claimed, 5) shouldBe 1
    }

    "com tudo resgatado a página inicial é a última" {
        val longLadder = (1..12).map { milestone("m$it", it * 10) }
        MilestoneRules.startPage(longLadder, longLadder.map { it.id }.toSet(), 5) shouldBe 2
    }

    "progresso até o marco vai de 0 a 100" {
        MilestoneRules.progress(milestone("1h", 60), 1800) shouldBe 50
        MilestoneRules.progress(milestone("1h", 60), 9000) shouldBe 100
        MilestoneRules.progress(milestone("1h", 60), 0) shouldBe 0
    }
})
