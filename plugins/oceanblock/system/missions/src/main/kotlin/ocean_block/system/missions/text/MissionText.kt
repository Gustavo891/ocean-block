package ocean_block.system.missions.text

import ocean_block.system.missions.catalog.DailyPool
import ocean_block.system.missions.model.Mission
import ocean_block.system.missions.model.MissionObjective
import ocean_block.system.missions.rules.MissionRules
import java.util.Locale

object MissionText {

    private val locale: Locale = Locale.of("pt", "BR")

    fun hud(current: MissionRules.Current?, trailDone: Boolean): String {
        if (current == null) {
            return if (trailDone) "<gray>Diárias concluídas. Volte amanhã!" else ""
        }
        val label = current.dailyIndex?.let { "Diária ${it + 1}/${DailyPool.DAILY_COUNT}:" } ?: "Missão:"
        val counter = counter(current.mission.objective, current.progress)
        val tail = if (counter.isEmpty()) "" else " <gray>$counter"
        return "<aqua>$label</aqua> <white>${current.mission.text}</white>$tail"
    }

    fun completed(mission: Mission, xp: Int): String =
        "<green>Missão concluída:</green> <white>${mission.text}</white>" + if (xp > 0) " <gold>+$xp XP" else ""

    fun requirement(objective: MissionObjective, recipeName: (String) -> String): String = when (objective) {
        is MissionObjective.CollectDebris ->
            if (objective.hookOnly) "Puxe ${objective.amount} detritos com o Gancho"
            else "Colete ${objective.amount} detritos (Gancho ou toque)"
        MissionObjective.BuildGenerator -> "Monte um gerador de pedra na sua ilha"
        is MissionObjective.BreakGeneratorBlocks -> objective.material
            ?.let { "Quebre ${objective.amount} ${ORES[it] ?: it.lowercase()} do gerador" }
            ?: "Quebre ${objective.amount} blocos do gerador à mão"
        is MissionObjective.Smelt -> "Derreta ${objective.amount} minérios brutos"
        is MissionObjective.Craft -> "Fabrique em /receitas: ${recipeName(objective.recipeId)}"
        is MissionObjective.CraftAny -> "Fabrique ${objective.amount} receitas em /receitas"
        is MissionObjective.ReachLevel -> "Chegue ao nível ${objective.level}"
        is MissionObjective.Sell -> "Venda $${number(objective.value)} em itens no /sell"
        is MissionObjective.IslandUpgrade -> "Compre o ${UPGRADES[objective.upgrade] ?: objective.upgrade} nível ${objective.level} em /ilha"
        is MissionObjective.KillMobs -> "Mate ${objective.amount} mobs"
    }

    fun reward(xp: Int): String = "+${number(xp.toLong())} XP de nível"

    private val ORES = mapOf(
        "COAL_ORE" to "minérios de carvão",
        "COPPER_ORE" to "minérios de cobre",
        "IRON_ORE" to "minérios de ferro",
        "GOLD_ORE" to "minérios de ouro",
        "REDSTONE_ORE" to "minérios de redstone",
        "LAPIS_ORE" to "minérios de lápis-lazúli",
        "EMERALD_ORE" to "minérios de esmeralda",
        "DIAMOND_ORE" to "minérios de diamante",
    )

    private val UPGRADES = mapOf("Generator" to "Gerador", "Members" to "Membros", "Size" to "Borda", "CropGrowth" to "Plantação")

    fun counter(objective: MissionObjective, progress: Long): String {
        val goal = objective.goal
        if (goal <= 1) return ""
        val shown = progress.coerceIn(0, goal)
        return when (objective) {
            is MissionObjective.Sell -> "$${number(shown)}/$${number(goal)}"
            else -> "${number(shown)}/${number(goal)}"
        }
    }

    fun number(value: Long): String = "%,d".format(locale, value)
}
