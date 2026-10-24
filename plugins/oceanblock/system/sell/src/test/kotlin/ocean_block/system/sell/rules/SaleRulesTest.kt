package ocean_block.system.sell.rules

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class SaleRulesTest : StringSpec({

    val prices = mapOf("kelp" to 0.5, "cod" to 2.0)
    val price = { key: String -> prices[key] ?: 0.0 }

    "soma preço vezes quantidade de cada item vendável" {
        val quote = SaleRules.quote(listOf(SaleLine(3, "kelp", 10), SaleLine(7, "cod", 4)), price)

        quote.total shouldBe 13.0
        quote.sold.map { it.slot } shouldContainExactly listOf(3, 7)
        quote.unsellable shouldBe emptyList()
    }

    "item sem preço volta para o jogador e não entra no total" {
        val quote = SaleRules.quote(listOf(SaleLine(1, "dirt", 64), SaleLine(2, "cod", 1)), price)

        quote.total shouldBe 2.0
        quote.unsellable.map { it.slot } shouldContainExactly listOf(1)
    }

    "cada venda carrega o valor só do próprio item" {
        val quote = SaleRules.quote(listOf(SaleLine(0, "kelp", 2), SaleLine(1, "cod", 3)), price)

        quote.sold.map { it.value } shouldContainExactly listOf(1.0, 6.0)
    }

    "sem itens não há venda" {
        val quote = SaleRules.quote(emptyList(), price)

        quote.total shouldBe 0.0
        quote.isEmpty shouldBe true
    }
})
