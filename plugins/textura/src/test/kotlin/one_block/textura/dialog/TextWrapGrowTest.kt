package one_block.textura.dialog

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import one_block.textura.dialog.layout.TextWrap
import one_block.textura.dialog.node.UiSize

class TextWrapGrowTest : StringSpec({

    val measure: (String) -> Int = { it.length * 4 }

    "quebra por palavras sem passar da largura" {
        TextWrap.lines("VENDA 3.000 COINS EM ITENS NO /SELL", 60, measure) shouldBe
            listOf("VENDA 3.000", "COINS EM ITENS", "NO /SELL")
    }

    "texto curto fica numa linha e palavra longa nao some" {
        TextWrap.lines("MATE 50 MOBS", 200, measure) shouldBe listOf("MATE 50 MOBS")
        TextWrap.lines("PALAVRAMUITOLONGA X", 20, measure) shouldBe listOf("PALAVRAMUITOLONGA", "X")
        TextWrap.lines("", 20, measure) shouldBe emptyList()
    }

    "fatia proporcional respeita o minimo e a largura" {
        UiSize.Share(0.5, 3).fraction shouldBe 0.5
    }
})
