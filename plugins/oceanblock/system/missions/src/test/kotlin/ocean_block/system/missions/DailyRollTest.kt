package ocean_block.system.missions

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import ocean_block.system.missions.catalog.DailyPool
import ocean_block.system.missions.rules.MissionRules
import java.time.LocalDate
import kotlin.random.Random

class DailyRollTest : StringSpec({

    val day = LocalDate.of(2026, 10, 25)

    "sorteia 3 diarias distintas do pool" {
        val dailies = MissionRules.rollDailies(day, 10, Random(1))

        dailies.size shouldBe 3
        dailies.map { it.id }.toSet().size shouldBe 3
        dailies.forEach { it.id shouldBeIn DailyPool.all.map { mission -> mission.id } }
        dailies.forEach {
            it.progress shouldBe 0
            it.done shouldBe false
        }
    }

    "nunca sorteia duas do mesmo grupo" {
        repeat(200) { seed ->
            val ids = MissionRules.rollDailies(day, 10, Random(seed)).map { it.id }
            ids.map { it.substringBeforeLast('_') }.toSet().size shouldBe 3
        }
    }

    "xp acompanha o nivel do sorteio" {
        MissionRules.rollDailies(day, 10, Random(1)).map { it.xp }.toSet() shouldBe setOf(474)
    }

    "xp minimo e 30" {
        MissionRules.rollDailies(day, 1, Random(1)).map { it.xp }.toSet() shouldBe setOf(30)
    }
})
