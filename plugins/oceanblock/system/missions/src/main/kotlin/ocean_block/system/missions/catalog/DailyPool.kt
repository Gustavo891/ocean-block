package ocean_block.system.missions.catalog

import ocean_block.system.missions.model.Mission
import ocean_block.system.missions.model.MissionObjective

object DailyPool {

    const val DAILY_COUNT = 3

    val all: List<Mission> = listOf(
        Mission("detritos_30", "Catador de sucata", "Colete 30 detritos", MissionObjective.CollectDebris(30, false), 0, "oak_planks"),
        Mission("detritos_80", "Maré cheia", "Colete 80 detritos", MissionObjective.CollectDebris(80, false), 0, "kelp"),
        Mission("gancho_15", "Mão no gancho", "Colete 15 detritos usando o gancho", MissionObjective.CollectDebris(15, true), 0, "fishing_rod"),
        Mission("gerador_64", "Picareta afiada", "Quebre 64 blocos do gerador", MissionObjective.BreakGeneratorBlocks(64, null), 0, "cobblestone"),
        Mission("gerador_128", "Britadeira", "Quebre 128 blocos do gerador", MissionObjective.BreakGeneratorBlocks(128, null), 0, "stone"),
        Mission("carvao_24", "Carvoeiro", "Quebre 24 minérios de carvão", MissionObjective.BreakGeneratorBlocks(24, "COAL_ORE"), 0, "coal"),
        Mission("cobre_16", "Veio de cobre", "Quebre 16 minérios de cobre", MissionObjective.BreakGeneratorBlocks(16, "COPPER_ORE"), 0, "raw_copper"),
        Mission("ferro_12", "Mineiro de ferro", "Quebre 12 minérios de ferro", MissionObjective.BreakGeneratorBlocks(12, "IRON_ORE"), 0, "raw_iron"),
        Mission("derreter_24", "Forja acesa", "Derreta 24 minérios brutos", MissionObjective.Smelt(24), 0, "iron_ingot"),
        Mission("receitas_3", "Artesão do dia", "Fabrique 3 receitas", MissionObjective.CraftAny(3), 0, "crafting_table"),
        Mission("venda_2000", "Feirante", "Venda $2.000 em itens", MissionObjective.Sell(2000), 0, "emerald"),
        Mission("venda_5000", "Magnata do porto", "Venda $5.000 em itens", MissionObjective.Sell(5000), 0, "emerald_block"),
        Mission("venda_10000", "Barão do sal", "Venda $10.000 em itens", MissionObjective.Sell(10000), 0, "gold_block"),
        Mission("mobs_20", "Patrulha noturna", "Mate 20 mobs", MissionObjective.KillMobs(20), 0, "entidade:zombie"),
        Mission("mobs_50", "Caçador de monstros", "Mate 50 mobs", MissionObjective.KillMobs(50), 0, "entidade:skeleton"),
        Mission("barril_3", "Tesouro à deriva", "Pesque 3 barris", MissionObjective.CollectDebris(3, false, "barril"), 0, "barrel"),
        Mission("recife_2", "Mergulho nos corais", "Pesque 2 recifes", MissionObjective.CollectDebris(2, false, "recife"), 0, "brain_coral_block"),
    )

    private val byId: Map<String, Mission> = all.associateBy { it.id }

    fun byId(id: String): Mission? = byId[id]

    fun group(id: String): String = id.substringBeforeLast('_')
}
