package ocean_block.system.missions.catalog

import ocean_block.system.missions.model.Mission
import ocean_block.system.missions.model.MissionObjective

object MissionTrail {

    val all: List<Mission> = listOf(
        Mission("gancho_10", "Colete 10 detritos usando o gancho", MissionObjective.CollectDebris(10, true), 50),
        Mission("gerador_pedra", "Faça um gerador de pedra", MissionObjective.BuildGenerator, 50),
        Mission("gerador_32", "Quebre 32 blocos do gerador", MissionObjective.BreakGeneratorBlocks(32, null), 60),
        Mission("fundicao", "Faça uma Fundição", MissionObjective.Craft("fundicao"), 80),
        Mission("nivel_3", "Chegue ao nível 3", MissionObjective.ReachLevel(3), 50),
        Mission("rede", "Faça uma Rede de Detritos", MissionObjective.Craft("rede"), 100),
        Mission("detritos_50", "Colete 50 detritos", MissionObjective.CollectDebris(50, false), 120),
        Mission("venda_1000", "Venda $1.000 em itens", MissionObjective.Sell(1000), 150),
        Mission("upgrade_gerador", "Compre o upgrade do gerador nível 1", MissionObjective.IslandUpgrade("Generator", 1), 200),
        Mission("ferro_10", "Quebre 10 minérios de ferro", MissionObjective.BreakGeneratorBlocks(10, "IRON_ORE"), 150),
        Mission("fundicao_melhorada", "Faça uma Fundição rápida", MissionObjective.Craft("fundicao_melhorada"), 300),
        Mission("gerador_energia", "Faça um Gerador de energia", MissionObjective.Craft("gerador_energia"), 600),
        Mission("minerador", "Faça um Minerador", MissionObjective.Craft("minerador"), 1000),
    )

    val size: Int get() = all.size

    fun at(index: Int): Mission? = all.getOrNull(index)
}
