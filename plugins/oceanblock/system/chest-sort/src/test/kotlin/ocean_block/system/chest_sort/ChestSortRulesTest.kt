package ocean_block.system.chest_sort

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private data class Fake(val material: String, val amount: Int, val max: Int = 64, val tag: String = "")

private object FakeKind : StackKind<Fake> {
    override fun same(a: Fake, b: Fake) = a.material == b.material && a.max == b.max && a.tag == b.tag
    override fun amount(stack: Fake) = stack.amount
    override fun maxStack(stack: Fake) = stack.max
    override fun name(stack: Fake) = stack.material
    override fun withAmount(stack: Fake, amount: Int) = stack.copy(amount = amount)
}

private fun sort(vararg slots: Fake?) = ChestSortRules.sort(slots.toList(), FakeKind)

private fun totals(slots: List<Fake?>) =
    slots.filterNotNull().groupBy { it.copy(amount = 0) }.mapValues { (_, stacks) -> stacks.sumOf { it.amount } }

class ChestSortRulesTest : StringSpec({

    "baú vazio continua vazio e do mesmo tamanho" {
        sort(null, null, null) shouldContainExactly listOf(null, null, null)
    }

    "junta stacks iguais e empurra os vazios para o fim" {
        sort(Fake("DIRT", 10), null, Fake("DIRT", 20)) shouldContainExactly
            listOf(Fake("DIRT", 30), null, null)
    }

    "divide no tamanho máximo do stack" {
        sort(Fake("DIRT", 50), Fake("DIRT", 50)) shouldContainExactly
            listOf(Fake("DIRT", 64), Fake("DIRT", 36))
    }

    "respeita stack de 16 e itens que não empilham" {
        sort(Fake("ENDER_PEARL", 10, 16), Fake("ENDER_PEARL", 10, 16), null) shouldContainExactly
            listOf(Fake("ENDER_PEARL", 16, 16), Fake("ENDER_PEARL", 4, 16), null)
        sort(Fake("IRON_SWORD", 1, 1), null, Fake("IRON_SWORD", 1, 1)) shouldContainExactly
            listOf(Fake("IRON_SWORD", 1, 1), Fake("IRON_SWORD", 1, 1), null)
    }

    "ordena pela quantidade total do tipo, do maior para o menor" {
        sort(Fake("STONE", 5), Fake("APPLE", 3), Fake("DIRT", 40), Fake("STONE", 30)) shouldContainExactly
            listOf(Fake("DIRT", 40), Fake("STONE", 35), Fake("APPLE", 3), null)
    }

    "a quantidade total conta todos os stacks do tipo, não o maior" {
        sort(Fake("DIRT", 60), Fake("COBBLESTONE", 40), Fake("COBBLESTONE", 40), null) shouldContainExactly
            listOf(Fake("COBBLESTONE", 64), Fake("COBBLESTONE", 16), Fake("DIRT", 60), null)
    }

    "empate desempata pelo nome do material" {
        sort(Fake("DIRT", 10), Fake("APPLE", 10), Fake("COAL", 10)) shouldContainExactly
            listOf(Fake("APPLE", 10), Fake("COAL", 10), Fake("DIRT", 10))
    }

    "item custom só junta com idêntico" {
        sort(Fake("PAPER", 1, tag = "broca_ouro"), Fake("PAPER", 5), Fake("PAPER", 2, tag = "broca_ouro")) shouldContainExactly
            listOf(Fake("PAPER", 5), Fake("PAPER", 3, tag = "broca_ouro"), null)
    }

    "mesmo material e nome com empate fica na ordem em que apareceu" {
        sort(Fake("PAPER", 4, tag = "b"), Fake("PAPER", 4, tag = "a")) shouldContainExactly
            listOf(Fake("PAPER", 4, tag = "b"), Fake("PAPER", 4, tag = "a"))
    }

    "quantidade zero conta como slot vazio" {
        sort(Fake("DIRT", 0), Fake("STONE", 2)) shouldContainExactly listOf(Fake("STONE", 2), null)
    }

    "stack acima do máximo não é dividido nem perde itens" {
        sort(Fake("DIRT", 100), Fake("DIRT", 100)) shouldContainExactly
            listOf(Fake("DIRT", 100), Fake("DIRT", 100))
        sort(Fake("DIRT", 100), Fake("DIRT", 10), null) shouldContainExactly
            listOf(Fake("DIRT", 100), Fake("DIRT", 10), null)
    }

    "baú cheio e misturado não perde nem duplica item" {
        val slots = (0 until 54).map { i ->
            when (i % 6) {
                0 -> null
                1 -> Fake("DIRT", 1 + i % 64)
                2 -> Fake("ENDER_PEARL", 1 + i % 16, 16)
                3 -> Fake("IRON_SWORD", 1, 1)
                4 -> Fake("PAPER", 1 + i % 64, tag = "rede")
                else -> Fake("PAPER", 1 + i % 64)
            }
        }
        val sorted = ChestSortRules.sort(slots, FakeKind)
        sorted.size shouldBe 54
        totals(sorted) shouldBe totals(slots)
        sorted.filterNotNull().all { it.amount in 1..it.max } shouldBe true
    }
})
