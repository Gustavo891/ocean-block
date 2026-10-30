package ocean_block.system.missions.catalog

import ocean_block.system.missions.model.Mission
import ocean_block.system.missions.model.MissionObjective

object MissionTrail {

    val all: List<Mission> = listOf(
        Mission("gancho_10", "Primeira pescaria", "Colete 10 detritos usando o gancho", MissionObjective.CollectDebris(10, true), 50, "fishing_rod"),
        Mission("gerador_pedra", "Fonte de pedra", "Faça um gerador de pedra", MissionObjective.BuildGenerator, 50, "lava_bucket"),
        Mission("gerador_32", "Mãos à obra", "Quebre 32 blocos do gerador", MissionObjective.BreakGeneratorBlocks(32, null), 60, "cobblestone"),
        Mission("nivel_3", "Aprendiz do mar", "Chegue ao nível 3", MissionObjective.ReachLevel(3), 50, "experience_bottle"),
        Mission("rede", "Rede armada", "Faça uma Rede de Detritos", MissionObjective.Craft("rede"), 100, "cobweb"),
        Mission("derreter_10", "Fogo na fornalha", "Derreta 10 minérios brutos", MissionObjective.Smelt(10), 80, "raw_iron"),
        Mission("carvao_10", "Primeiro carvão", "Quebre 10 minérios de carvão", MissionObjective.BreakGeneratorBlocks(10, "COAL_ORE"), 80, "coal"),
        Mission("detritos_50", "Catador do mar", "Colete 50 detritos", MissionObjective.CollectDebris(50, false), 120, "oak_planks"),
        Mission("venda_1000", "Primeiro comércio", "Venda $1.000 em itens", MissionObjective.Sell(1000), 150, "emerald"),
        Mission("barril_2", "Caça ao tesouro", "Pesque 2 barris", MissionObjective.CollectDebris(2, false, "barril"), 120, "barrel"),
        Mission("nivel_5", "Marinheiro", "Chegue ao nível 5", MissionObjective.ReachLevel(5), 150, "experience_bottle"),
        Mission("mobs_10", "Primeira patrulha", "Mate 10 mobs", MissionObjective.KillMobs(10), 120, "entidade:zombie"),
        Mission("ferro_10", "Veio de ferro", "Quebre 10 minérios de ferro", MissionObjective.BreakGeneratorBlocks(10, "IRON_ORE"), 150, "raw_iron"),
        Mission("venda_3000", "Mercador", "Venda $3.000 em itens", MissionObjective.Sell(3000), 200, "gold_ingot"),
        Mission("nivel_6", "Capitão da ilha", "Chegue ao nível 6", MissionObjective.ReachLevel(6), 300, "spyglass"),
    )

    val size: Int get() = all.size

    fun at(index: Int): Mission? = all.getOrNull(index)
}
