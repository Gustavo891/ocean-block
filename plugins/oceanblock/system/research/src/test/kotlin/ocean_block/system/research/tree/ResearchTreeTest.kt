package ocean_block.system.research.tree

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import ocean_block.system.research.model.ResearchCategory

class ResearchTreeTest : StringSpec({

    "materiais dos icones incluem os que nao sao resultado de receita" {
        ResearchTree.iconMaterials() shouldContainExactlyInAnyOrder listOf("loom", "blast_furnace", "cartography_table", "hopper", "iron_pickaxe", "furnace", "daylight_detector", "observer")
    }

    "arvore v1 tem as pesquisas do spec" {
        ResearchTree.all.map { it.id } shouldContainExactlyInAnyOrder listOf(
            "rede", "fundicao_melhorada", "bancada_auxiliar", "fundicao_funil", "bancada_auxiliar_2",
            "picareta_fundidora", "gerador_energia", "painel_solar", "fornalha_eletrica", "minerador",
        )
    }

    "maquinas ficam na categoria Maquinas e dependem do gerador" {
        listOf("gerador_energia", "painel_solar", "fornalha_eletrica", "minerador").forEach {
            ResearchTree.byId.getValue(it).category shouldBe ResearchCategory.MAQUINAS
            ResearchTree.byId.getValue(it).unlocksRecipes shouldBe listOf(it)
        }
        listOf("painel_solar", "fornalha_eletrica", "minerador").forEach {
            ResearchTree.byId.getValue(it).requires shouldBe listOf("gerador_energia")
        }
        ResearchTree.byId.getValue("gerador_energia").requires shouldBe listOf("fundicao_melhorada")
        ResearchTree.byId.getValue("picareta_fundidora").category shouldBe ResearchCategory.FUNDICAO
    }

    "minerador custa 20 ferro, 6 ouro e 12 redstone em 5 minutos" {
        val miner = ResearchTree.byId.getValue("minerador")
        miner.cost.associate { it.key to it.amount } shouldBe mapOf("iron_ingot" to 20, "gold_ingot" to 6, "redstone" to 12)
        miner.durationSeconds shouldBe 300
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
        ResearchTree.byId.getValue("bancada_auxiliar_2").requires shouldContainExactlyInAnyOrder listOf("bancada_auxiliar", "fundicao_melhorada")
    }
})
