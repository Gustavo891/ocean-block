package ocean_block.system.stations.sieve

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import kotlin.random.Random

class SieveRollerTest : StringSpec({

    "so sai o que acerta a chance, com quantidade entre min e max" {
        repeat(20) { seed ->
            val result = SieveRoller.roll(
                listOf(SieveDrop("pedaco_ferro", 1, 3, 1.0), SieveDrop("DIAMOND", 1, 1, 0.0)),
                Random(seed),
            )

            result.map { it.first } shouldBe listOf("pedaco_ferro")
            result.single().second shouldBeInRange 1..3
        }
    }

    "nenhum acerto nao devolve nada" {
        SieveRoller.roll(listOf(SieveDrop("COAL", 1, 1, 0.0)), Random(1)).shouldBeEmpty()
    }
})
