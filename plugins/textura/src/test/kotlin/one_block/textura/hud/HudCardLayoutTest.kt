package one_block.textura.hud

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import java.util.function.ToIntFunction

class HudCardLayoutTest : StringSpec({

    val width = ToIntFunction<String> { it.length * 6 }

    fun tiles(text: String): List<String> =
        Regex("<glyph:mare_(\\d)>").findAll(text).map { it.groupValues[1] }.toList()

    "linha de cima manda a largura quando é a maior" {
        val card = HudCard("a".repeat(30), "+9", "1/2", 0.5f, "mare")
        val text = HudCardLayout.compose(card, width)
        text shouldStartWith "a".repeat(30) + "<shift:8>+9<shift:-200>"
        tiles(text).size shouldBe (200 - 8 - 18) / 8
        text shouldEndWith "<font:minecraft:bossbar2>1/2</font>"
    }

    "barra mínima quando a tarefa é curta" {
        val card = HudCard("ab", "", "3/10", 0f, "mare")
        val text = HudCardLayout.compose(card, width)
        val inner = HudCardLayout.MIN_BAR + HudCardLayout.GAP + 24
        text shouldStartWith "ab<shift:${inner - 12}><shift:-$inner>"
        tiles(text) shouldBe List(HudCardLayout.MIN_BAR / 8) { "0" }
    }

    "preenchimento corta um bloco no meio" {
        val card = HudCard("ab", "", "x", 0.5f, "mare")
        val text = HudCardLayout.compose(card, width)
        val count = tiles(text).size
        val filled = HudCardLayout.filled(count, 0.5f)
        tiles(text) shouldBe List(count) { index -> (filled - index * 8).coerceIn(0, 8).toString() }
    }

    "progresso fora de 0..1 é limitado" {
        HudCardLayout.filled(8, 1.7f) shouldBe 64
        HudCardLayout.filled(8, -1f) shouldBe 0
    }
})
