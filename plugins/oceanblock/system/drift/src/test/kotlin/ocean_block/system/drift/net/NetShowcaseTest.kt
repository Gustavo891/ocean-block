package ocean_block.system.drift.net

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldBeUnique
import io.kotest.matchers.shouldBe

class NetShowcaseTest : StringSpec({

    val stored = listOf("STRING", "OAK_PLANKS", "STRING", "KELP", "STICK", "IRON_NUGGET", "OAK_PLANKS")

    "mostra ate 4 tipos diferentes do que esta guardado" {
        val shown = NetShowcase.pick(stored, seed = 7)
        shown shouldHaveSize 4
        shown.shouldBeUnique()
        stored.containsAll(shown) shouldBe true
    }

    "a escolha nao muda enquanto o conteudo e a rede sao os mesmos" {
        NetShowcase.pick(stored, seed = 7) shouldBe NetShowcase.pick(stored.reversed(), seed = 7)
    }

    "com poucos tipos mostra todos e vazia nao mostra nada" {
        NetShowcase.pick(listOf("KELP", "KELP", "STICK"), seed = 1) shouldContainExactlyInAnyOrder listOf("KELP", "STICK")
        NetShowcase.pick(emptyList(), seed = 1).shouldBeEmpty()
    }
})
