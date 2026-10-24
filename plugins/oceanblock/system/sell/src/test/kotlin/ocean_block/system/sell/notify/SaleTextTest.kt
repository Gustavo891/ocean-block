package ocean_block.system.sell.notify

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SaleTextTest : StringSpec({

    "mostra o lucro e quantos itens foram vendidos" {
        SaleText.line("1,2K", 37) shouldBe "<green>+<dark_green>$<white>1,2K <gray>(37 itens vendidos)"
    }

    "usa o singular para um item só" {
        SaleText.line("4", 1) shouldBe "<green>+<dark_green>$<white>4 <gray>(1 item vendido)"
    }
})
