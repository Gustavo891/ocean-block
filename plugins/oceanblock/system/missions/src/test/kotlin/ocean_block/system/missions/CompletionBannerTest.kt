package ocean_block.system.missions

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import ocean_block.system.missions.hud.CompletionBanner

class CompletionBannerTest : StringSpec({

    "sem conclusao nao ha faixa" {
        CompletionBanner(80).current(0) shouldBe null
    }

    "a faixa dura o tempo configurado e depois some" {
        val banner = CompletionBanner(80)
        banner.push(listOf("A"), 100)

        banner.current(100) shouldBe "A"
        banner.current(179) shouldBe "A"
        banner.current(180) shouldBe null
    }

    "varias conclusoes aparecem uma depois da outra" {
        val banner = CompletionBanner(80)
        banner.push(listOf("A", "B"), 0)

        banner.current(10) shouldBe "A"
        banner.current(80) shouldBe "B"
        banner.current(160) shouldBe null
        banner.ends() shouldBe listOf(80L, 160L)
    }

    "conclusao nova entra depois da que ainda esta na tela" {
        val banner = CompletionBanner(80)
        banner.push(listOf("A"), 0)
        banner.push(listOf("B"), 40)

        banner.current(60) shouldBe "A"
        banner.current(100) shouldBe "B"
        banner.current(160) shouldBe null
    }
})
