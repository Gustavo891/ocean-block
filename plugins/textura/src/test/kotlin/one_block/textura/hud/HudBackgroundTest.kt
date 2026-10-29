package one_block.textura.hud

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class HudBackgroundTest : StringSpec({

    "fundo padrão centraliza o texto como antes" {
        val plate = HudBackground.Glyphs("a", "b", "c", 6, 6, 6)
        HudBackground.textStart(36, 20, plate) shouldBe (36 - 20) / 2
        HudBackground.textStart(37, 20, plate) shouldBe (37 - 20) / 2
    }

    "selo largo à esquerda centraliza o texto só no corpo" {
        val seal = HudBackground.Glyphs("a", "b", "c", 14, 6, 6)
        HudBackground.textStart(50, 24, seal) shouldBe 14 + (50 - 14 - 6 - 24) / 2
    }

    "conteúdo só com texto usa o fundo padrão" {
        HudContent("x").background shouldBe HudBackground.DEFAULT_PREFIX
        HudContent("x", "hud_invasao").background shouldBe "hud_invasao"
    }
})
