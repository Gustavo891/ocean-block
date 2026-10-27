package ocean_block.system.missions.catalog

import ocean_block.system.missions.model.Mission
import ocean_block.system.missions.model.MissionObjective

object DailyPool {

    const val DAILY_COUNT = 3

    val all: List<Mission> = listOf(
        Mission("detritos_30", "Colete 30 detritos", MissionObjective.CollectDebris(30, false), 0, "oak_planks"),
        Mission("gancho_15", "Colete 15 detritos usando o gancho", MissionObjective.CollectDebris(15, true), 0, "fishing_rod"),
        Mission("gerador_64", "Quebre 64 blocos do gerador", MissionObjective.BreakGeneratorBlocks(64, null), 0, "cobblestone"),
        Mission("gerador_128", "Quebre 128 blocos do gerador", MissionObjective.BreakGeneratorBlocks(128, null), 0, "stone"),
        Mission("receitas_3", "Fabrique 3 receitas", MissionObjective.CraftAny(3), 0, "crafting_table"),
        Mission("venda_2000", "Venda $2.000 em itens", MissionObjective.Sell(2000), 0, "emerald"),
        Mission("venda_5000", "Venda $5.000 em itens", MissionObjective.Sell(5000), 0, "emerald_block"),
        Mission("mobs_20", "Mate 20 mobs", MissionObjective.KillMobs(20), 0, "iron_sword"),
        Mission("mobs_50", "Mate 50 mobs", MissionObjective.KillMobs(50), 0, "diamond_sword"),
    )

    private val byId: Map<String, Mission> = all.associateBy { it.id }

    fun byId(id: String): Mission? = byId[id]

    fun group(id: String): String = id.substringBeforeLast('_')
}
