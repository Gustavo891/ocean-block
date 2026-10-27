package ocean_block.system.missions.catalog

import ocean_block.system.missions.model.Mission
import ocean_block.system.missions.model.MissionObjective

object MissionTrail {

    val all: List<Mission> = listOf(
        Mission("gancho_10", "Colete 10 detritos usando o gancho", MissionObjective.CollectDebris(10, true), 50, "fishing_rod"),
        Mission("gerador_pedra", "Faça um gerador de pedra", MissionObjective.BuildGenerator, 50, "lava_bucket"),
        Mission("gerador_32", "Quebre 32 blocos do gerador", MissionObjective.BreakGeneratorBlocks(32, null), 60, "cobblestone"),
        Mission("derreter_10", "Derreta 10 minérios brutos", MissionObjective.Smelt(10), 80, "raw_iron"),
        Mission("nivel_3", "Chegue ao nível 3", MissionObjective.ReachLevel(3), 50, "experience_bottle"),
        Mission("rede", "Faça uma Rede de Detritos", MissionObjective.Craft("rede"), 100, "cobweb"),
        Mission("detritos_50", "Colete 50 detritos", MissionObjective.CollectDebris(50, false), 120, "oak_planks"),
        Mission("venda_1000", "Venda $1.000 em itens", MissionObjective.Sell(1000), 150, "emerald"),
        Mission("upgrade_gerador", "Compre o upgrade do gerador nível 1", MissionObjective.IslandUpgrade("Generator", 1), 200, "anvil"),
        Mission("ferro_10", "Quebre 10 minérios de ferro", MissionObjective.BreakGeneratorBlocks(10, "IRON_ORE"), 150, "iron_ore"),
        Mission("fornalha_eletrica", "Faça uma Fornalha elétrica", MissionObjective.Craft("fornalha_eletrica"), 300, "blast_furnace"),
        Mission("gerador_energia", "Faça um Gerador Marinho", MissionObjective.Craft("gerador_energia"), 600, "furnace"),
        Mission("minerador", "Faça um Minerador", MissionObjective.Craft("minerador"), 1000, "observer"),
    )

    val size: Int get() = all.size

    fun at(index: Int): Mission? = all.getOrNull(index)
}
