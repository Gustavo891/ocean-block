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

    val all: List<ResearchDefinition> = listOf(
        ResearchDefinition(
            "rede", "Rede de Detritos", "Coleta sozinha o que passa perto do raft.",
            ResearchCategory.COLETA, "item:loom", listOf(planks(10), string(6)), 20, emptyList(), listOf("rede"),
        ),
        ResearchDefinition(
            "peneira_ferro", "Peneira de ferro", "Menos etapas e chance de redstone e diamante.",
            ResearchCategory.COLETA, "item:composter", listOf(iron(4), string(8)), 60, emptyList(), listOf("peneira_ferro"),
        ),
        ResearchDefinition(
            "fundicao_melhorada", "Fundição melhorada", "Funde duas vezes mais rápido e pode dobrar o lingote.",
            ResearchCategory.FUNDICAO, "item:blast_furnace", listOf(iron(6), copper(4), clay(8)), 120, emptyList(), listOf("fundicao_melhorada"),
        ),
        ResearchDefinition(
            "bancada_auxiliar", "Bancada auxiliar", "Libera uma segunda pesquisa ao mesmo tempo.",
            ResearchCategory.RAFT, "item:cartography_table", listOf(planks(12), iron(6)), 90, listOf("rede"), emptyList(), extraSlots = 1,
        ),
        ResearchDefinition(
            "peneira_diamante", "Peneira de diamante", "A peneira mais rápida, com as melhores chances.",
            ResearchCategory.COLETA, "item:composter", listOf(diamond(2), iron(8)), 240, listOf("peneira_ferro"), listOf("peneira_diamante"),
        ),
        ResearchDefinition(
            "peneira_automatica", "Peneira automática", "Peneira sozinha entre um baú acima e um abaixo.",
            ResearchCategory.COLETA, "item:hopper", listOf(iron(10), redstone(4)), 300, listOf("peneira_ferro"), listOf("peneira_automatica"),
        ),
        ResearchDefinition(
            "fundicao_funil", "Fundição com funil", "Recebe minério e combustível por funis e entrega lingotes.",
            ResearchCategory.FUNDICAO, "item:hopper", listOf(iron(10), copper(4), redstone(2)), 300, listOf("fundicao_melhorada"), listOf("fundicao_funil"),
        ),
        ResearchDefinition(
            "bancada_auxiliar_2", "Bancada auxiliar II", "Libera uma terceira pesquisa ao mesmo tempo.",
            ResearchCategory.RAFT, "item:cartography_table", listOf(iron(12), gold(6), diamond(2)), 300,
            listOf("bancada_auxiliar", "peneira_diamante"), emptyList(), extraSlots = 1,
        ),
    )

    val byId: Map<String, ResearchDefinition> = all.associateBy { it.id }
}
