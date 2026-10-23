package ocean_block.system.research.rules

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ocean_block.system.research.model.IslandResearch
import ocean_block.system.research.model.ResearchStatus
import ocean_block.system.research.tree.ResearchTree
import java.util.UUID

private const val T0 = 1_000_000L
private val tree = ResearchTree.byId
private fun def(id: String) = tree.getValue(id)
private fun fresh() = IslandResearch(UUID.randomUUID())

private fun complete(state: IslandResearch, id: String, now: Long = T0) {
    val missing = ResearchRules.missing(def(id), state)
    ResearchRules.deliver(def(id), state, missing, now, tree)
}

class ResearchRulesTest : StringSpec({

    "entrega limita ao que falta" {
        val state = fresh()

        val taken = ResearchRules.deliver(def("rede"), state, mapOf("planks" to 25, "string" to 2), T0, tree)

        taken shouldBe mapOf("planks" to 10, "string" to 2)
        ResearchRules.missing(def("rede"), state) shouldBe mapOf("string" to 4)
        ResearchRules.status(def("rede"), state, T0) shouldBe ResearchStatus.Available
    }

    "completar a entrega comeca a pesquisa na hora" {
        val state = fresh()
        ResearchRules.deliver(def("rede"), state, mapOf("planks" to 10, "string" to 2), T0, tree)

        val taken = ResearchRules.deliver(def("rede"), state, mapOf("string" to 9), T0 + 5, tree)

        taken shouldBe mapOf("string" to 4)
        ResearchRules.status(def("rede"), state, T0 + 5) shouldBe ResearchStatus.Running(20_000)
    }

    "com uma vaga a segunda pesquisa completa espera na fila" {
        val state = fresh()
        complete(state, "rede")
        complete(state, "fundicao_melhorada")

        ResearchRules.status(def("rede"), state, T0).shouldBeInstanceOf<ResearchStatus.Running>()
        ResearchRules.status(def("fundicao_melhorada"), state, T0) shouldBe ResearchStatus.Queued
    }

    "conclui no fim e a fila comeca de onde a anterior terminou" {
        val state = fresh()
        complete(state, "rede")
        complete(state, "fundicao_melhorada")

        ResearchRules.completeDue(state, T0 + 19_999, tree).shouldBeEmpty()
        ResearchRules.completeDue(state, T0 + 20_000, tree) shouldBe listOf("rede")
        ResearchRules.status(def("fundicao_melhorada"), state, T0 + 20_000) shouldBe ResearchStatus.Running(120_000)
        ResearchRules.status(def("rede"), state, T0 + 20_000) shouldBe ResearchStatus.Done
    }

    "conclui o que venceu enquanto offline" {
        val state = fresh()
        complete(state, "rede")
        complete(state, "fundicao_melhorada")

        ResearchRules.completeDue(state, T0 + 10 * 60_000, tree) shouldBe listOf("rede", "fundicao_melhorada")
        state.running.shouldBeEmpty()
        state.queue.shouldBeEmpty()
    }

    "bancada auxiliar libera a segunda vaga" {
        val state = fresh()
        state.completed += listOf("rede", "bancada_auxiliar", "fundicao_melhorada")

        ResearchRules.slots(state, tree) shouldBe 2
        complete(state, "fundicao_funil")
        complete(state, "bancada_auxiliar_2")
        ResearchRules.status(def("bancada_auxiliar_2"), state, T0).shouldBeInstanceOf<ResearchStatus.Running>()
    }

    "sem pre-requisito fica bloqueada e nao aceita entrega" {
        val state = fresh()

        ResearchRules.status(def("fundicao_funil"), state, T0) shouldBe ResearchStatus.Locked(listOf("fundicao_melhorada"))
        ResearchRules.deliver(def("fundicao_funil"), state, mapOf("iron_ingot" to 10), T0, tree) shouldBe emptyMap()
    }
})
