package ocean_block.system.scoreboard.level

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class LevelTextTest : StringSpec({

    "o selo do nível usa o fundo do tamanho do número e volta o texto para dentro dele" {
        LevelText.badge(7) shouldBe "<glyph:nivel_badge_1><shift:-8><white>7<shift:2>"
        LevelText.badge(43) shouldBe "<glyph:nivel_badge_2><shift:-14><white>43<shift:2>"
        LevelText.badge(1250) shouldBe "<glyph:nivel_badge_4><shift:-26><white>1250<shift:2>"
    }

    "o selo limita o nível entre 0 e 9999" {
        LevelText.badge(-3) shouldBe "<glyph:nivel_badge_1><shift:-8><white>0<shift:2>"
        LevelText.badge(123456) shouldBe "<glyph:nivel_badge_4><shift:-26><white>9999<shift:2>"
    }

    "a barra enche um segmento a cada décimo do progresso" {
        LevelText.bar(0.0) shouldBe "<glyph:nivel_barra_0>"
        LevelText.bar(0.3341) shouldBe "<glyph:nivel_barra_3>"
        LevelText.bar(0.999) shouldBe "<glyph:nivel_barra_9>"
        LevelText.bar(1.0) shouldBe "<glyph:nivel_barra_10>"
    }

    "a barra ignora progresso fora de 0 a 1" {
        LevelText.bar(-0.5) shouldBe "<glyph:nivel_barra_0>"
        LevelText.bar(2.0) shouldBe "<glyph:nivel_barra_10>"
    }

    "o percentual usa vírgula e duas casas" {
        LevelText.percent(0.3341) shouldBe "33,41%"
        LevelText.percent(0.0) shouldBe "0,00%"
        LevelText.percent(1.0) shouldBe "100,00%"
        LevelText.percent(1.7) shouldBe "100,00%"
    }
})
