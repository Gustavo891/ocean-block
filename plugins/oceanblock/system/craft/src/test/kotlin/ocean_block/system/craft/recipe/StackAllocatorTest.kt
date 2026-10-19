package ocean_block.system.craft.recipe

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.bukkit.Material

private data class Pilha(val tipo: String, val quantidade: Int)

private fun allocate(items: List<Pilha?>, vararg needs: Requirement<Pilha>) =
    StackAllocator.allocate(items, Pilha::quantidade, needs.toList())

private fun count(items: List<Pilha?>, accepts: (Pilha) -> Boolean) =
    StackAllocator.count(items, Pilha::quantidade, accepts)

private fun precisa(amount: Int, vararg tipos: String) = Requirement<Pilha>(amount) { it.tipo in tipos }

class StackAllocatorTest : StringSpec({

    "conta somando todas as pilhas que combinam" {
        val items = listOf(Pilha("linha", 3), null, Pilha("terra", 9), Pilha("linha", 4))

        count(items) { it.tipo == "linha" } shouldBe 7
    }

    "conta zero sem pilhas que combinam" {
        count(listOf(null, Pilha("terra", 5))) { it.tipo == "linha" } shouldBe 0
    }

    "conta pilhas de tipos diferentes aceitos pelo mesmo ingrediente" {
        val items = listOf(Pilha("carvalho", 1), Pilha("betula", 2))

        count(items) { it.tipo == "carvalho" || it.tipo == "betula" } shouldBe 3
    }

    "consome a quantidade exata atravessando pilhas" {
        val items = listOf(Pilha("carvalho", 2), Pilha("linha", 5), Pilha("betula", 4))

        val result = allocate(items, precisa(3, "carvalho", "betula"), precisa(2, "linha"))

        result.complete shouldBe true
        result.remaining shouldContainExactly listOf(0, 3, 3)
    }

    "alocacao nao altera as pilhas de entrada" {
        val items = listOf(Pilha("linha", 5))

        allocate(items, precisa(2, "linha"))

        items shouldContainExactly listOf(Pilha("linha", 5))
    }

    "fica incompleta quando falta um ingrediente mesmo com o outro sobrando" {
        val items = listOf(Pilha("linha", 5), Pilha("carvalho", 2))

        val result = allocate(items, precisa(2, "linha"), precisa(3, "carvalho"))

        result.complete shouldBe false
    }

    "a mesma pilha nao serve dois ingredientes" {
        val result = allocate(listOf(Pilha("carvalho", 3)), precisa(2, "carvalho"), precisa(2, "carvalho"))

        result.complete shouldBe false
        result.taken shouldContainExactly listOf(2, 1)
        result.missing shouldContainExactly listOf(0, 1)
    }

    "faltas dizem quanto se tem e quanto falta de cada ingrediente" {
        val items = listOf(Pilha("linha", 5), Pilha("carvalho", 1))

        val result = allocate(items, precisa(2, "linha"), precisa(3, "carvalho"))

        result.taken shouldContainExactly listOf(2, 1)
        result.missing shouldContainExactly listOf(0, 2)
    }

    "sem faltas quando tudo esta no inventario" {
        val items = listOf(Pilha("linha", 2), Pilha("carvalho", 3))

        val result = allocate(items, precisa(2, "linha"), precisa(3, "carvalho"))

        result.missing shouldContainExactly listOf(0, 0)
        result.remaining shouldContainExactly listOf(0, 0)
    }

    "slots vazios ficam com zero" {
        allocate(listOf(null, Pilha("linha", 1)), precisa(1, "linha")).remaining shouldContainExactly listOf(0, 0)
    }

    "ingrediente com quantidade nao positiva e rejeitado" {
        runCatching { Ingredient.material(Material.STRING, "Linha", 0) }.isFailure shouldBe true
    }

    "rotulo usa o plural fora de um" {
        val linha = Ingredient.material(Material.STRING, "Linha", 2, plural = "Linhas")

        linha.labelFor(1) shouldBe "Linha"
        linha.labelFor(12) shouldBe "Linhas"
    }
})
