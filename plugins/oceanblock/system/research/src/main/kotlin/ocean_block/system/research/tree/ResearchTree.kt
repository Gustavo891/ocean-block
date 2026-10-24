package ocean_block.system.research.tree

import ocean_block.system.research.model.ResearchCategory
import ocean_block.system.research.model.ResearchCost
import ocean_block.system.research.model.ResearchDefinition

object ResearchTree {

    private fun planks(n: Int) = ResearchCost("planks", "Tábua", "Tábuas", n)
    private fun string(n: Int) = ResearchCost("string", "Linha", "Linhas", n)
    private fun iron(n: Int) = ResearchCost("iron_ingot", "Lingote de ferro", "Lingotes de ferro", n)
    private fun copper(n: Int) = ResearchCost("copper_ingot", "Lingote de cobre", "Lingotes de cobre", n)
    private fun gold(n: Int) = ResearchCost("gold_ingot", "Lingote de ouro", "Lingotes de ouro", n)
    private fun clay(n: Int) = ResearchCost("clay_ball", "Argila", "Argilas", n)
    private fun diamond(n: Int) = ResearchCost("diamond", "Diamante", "Diamantes", n)
    private fun redstone(n: Int) = ResearchCost("redstone", "Redstone", "Redstone", n)
    private fun coal(n: Int) = ResearchCost("coal", "Carvão", "Carvões", n)
    private fun lapis(n: Int) = ResearchCost("lapis_lazuli", "Lápis-lazúli", "Lápis-lazúli", n)

    val all: List<ResearchDefinition> = listOf(
        ResearchDefinition(
            "rede", "Rede de Detritos", "Coleta sozinha o que passa perto do raft.",
            ResearchCategory.COLETA, "item:loom", listOf(planks(10), string(6)), 20, emptyList(), listOf("rede"),
        ),
        ResearchDefinition(
            "fundicao_melhorada", "Fundição rápida", "Funde duas vezes mais rápido e pode dobrar o lingote.",
            ResearchCategory.FUNDICAO, "item:furnace", listOf(iron(6), copper(4), clay(8)), 120, emptyList(), listOf("fundicao_melhorada"),
        ),
        ResearchDefinition(
            "bancada_auxiliar", "Bancada auxiliar", "Libera uma segunda pesquisa ao mesmo tempo.",
            ResearchCategory.RAFT, "item:cartography_table", listOf(planks(12), iron(6)), 90, listOf("rede"), emptyList(), extraSlots = 1,
        ),
        ResearchDefinition(
            "fundicao_funil", "Fundição com funil", "Recebe minério e combustível por funis e entrega lingotes.",
            ResearchCategory.FUNDICAO, "item:hopper", listOf(iron(10), copper(4), redstone(2)), 300, listOf("fundicao_melhorada"), listOf("fundicao_funil"),
        ),
        ResearchDefinition(
            "bancada_auxiliar_2", "Bancada auxiliar II", "Libera uma terceira pesquisa ao mesmo tempo.",
            ResearchCategory.RAFT, "item:cartography_table", listOf(iron(12), gold(6), diamond(2)), 300,
            listOf("bancada_auxiliar", "fundicao_melhorada"), emptyList(), extraSlots = 1,
        ),
        ResearchDefinition(
            "picareta_fundidora", "Picareta fundidora", "Minério quebrado já sai em lingote.",
            ResearchCategory.FUNDICAO, "item:iron_pickaxe", listOf(iron(8), gold(4), coal(16)), 120, listOf("fundicao_melhorada"), listOf("picareta_fundidora"),
        ),
        ResearchDefinition(
            "gerador_energia", "Gerador de energia", "Queima combustível e alimenta máquinas a até 8 blocos.",
            ResearchCategory.MAQUINAS, "item:furnace", listOf(iron(12), copper(8), redstone(8)), 180, listOf("fundicao_melhorada"), listOf("gerador_energia"),
        ),
        ResearchDefinition(
            "painel_solar", "Painel solar", "Gera um pouco de energia de dia, sem combustível.",
            ResearchCategory.MAQUINAS, "item:daylight_detector", listOf(copper(12), lapis(8), redstone(6)), 180, listOf("gerador_energia"), listOf("painel_solar"),
        ),
        ResearchDefinition(
            "fornalha_eletrica", "Fornalha elétrica", "Derrete 4 itens ao mesmo tempo, 4 vezes mais rápido.",
            ResearchCategory.MAQUINAS, "item:blast_furnace", listOf(iron(16), gold(8), redstone(12)), 300, listOf("gerador_energia"), listOf("fornalha_eletrica"),
        ),
        ResearchDefinition(
            "minerador", "Minerador", "Quebra sozinho a pedra e o minério logo abaixo.",
            ResearchCategory.MAQUINAS, "item:observer", listOf(iron(20), gold(6), redstone(12)), 300, listOf("gerador_energia"), listOf("minerador"),
        ),
    )

    val byId: Map<String, ResearchDefinition> = all.associateBy { it.id }

    fun iconMaterials(): List<String> = all.map { it.icon.removePrefix("item:") }.distinct()
}
