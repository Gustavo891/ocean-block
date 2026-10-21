package ocean_block.system.research.tree

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe

class ResearchTreeTest : StringSpec({

    "materiais dos icones incluem os que nao sao resultado de receita" {
        ResearchTree.iconMaterials() shouldContainExactlyInAnyOrder listOf("loom", "composter", "blast_furnace", "cartography_table", "hopper")
    }

    "arvore v1 tem as pesquisas do spec" {
        ResearchTree.all.map { it.id } shouldContainExactlyInAnyOrder listOf(
            "rede", "peneira_ferro", "fundicao_melhorada", "bancada_auxiliar",
            "peneira_diamante", "peneira_automatica", "fundicao_funil", "bancada_auxiliar_2",
        )
    }

    "todo pre-requisito existe e nenhuma pesquisa passa de 5 minutos" {
        ResearchTree.all.forEach { definition ->
            ResearchTree.byId.keys shouldContainAll definition.requires
            definition.durationSeconds shouldBeLessThanOrEqual 300
        }
    }

    "nao ha ciclo de pre-requisitos" {
        fun depth(id: String, seen: Set<String>): Int {
            check(id !in seen) { "ciclo em $id" }
            return 1 + (ResearchTree.byId.getValue(id).requires.maxOfOrNull { depth(it, seen + id) } ?: 0)
        }
        ResearchTree.all.forEach { depth(it.id, emptySet()) }
    }

    "rede custa 10 tabuas e 6 linhas e leva 20 segundos" {
        val rede = ResearchTree.byId.getValue("rede")
        rede.cost.associate { it.key to it.amount } shouldBe mapOf("planks" to 10, "string" to 6)
        rede.durationSeconds shouldBe 20
        rede.unlocksRecipes shouldBe listOf("rede")
    }

    "bancadas somam uma vaga cada" {
        ResearchTree.byId.getValue("bancada_auxiliar").extraSlots shouldBe 1
        ResearchTree.byId.getValue("bancada_auxiliar_2").requires shouldContainExactlyInAnyOrder listOf("bancada_auxiliar", "peneira_diamante")
    }
})
