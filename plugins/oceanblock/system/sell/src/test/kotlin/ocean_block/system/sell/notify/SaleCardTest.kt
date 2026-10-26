package ocean_block.system.sell.notify

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly

class SaleCardTest : StringSpec({

    "o toast de venda tem o lucro em verde ao lado do selo e os itens vendidos em branco na linha de baixo" {
        SaleCard.lines("1,2K", 37) shouldContainExactly listOf(
            ToastLine("<green>+$1,2K</green>", "glyph:toast_venda"),
            ToastLine("<white>37 itens vendidos</white>", "glyph:toast_linha"),
        )
    }

    "usa o singular para um item só" {
        SaleCard.lines("4", 1) shouldContainExactly listOf(
            ToastLine("<green>+$4</green>", "glyph:toast_venda"),
            ToastLine("<white>1 item vendido</white>", "glyph:toast_linha"),
        )
    }
})
