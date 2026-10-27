package ocean_block.system.missions.rules

import ocean_block.system.level.curve.LevelCurve
import ocean_block.system.missions.catalog.DailyPool
import ocean_block.system.missions.catalog.MissionTrail
import ocean_block.system.missions.model.DailyMission
import ocean_block.system.missions.model.Mission
import ocean_block.system.missions.model.MissionObjective
import ocean_block.system.missions.model.MissionSignal
import ocean_block.system.missions.model.PlayerMissions
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

object MissionRules {

    private const val DAILY_XP_SHARE = 0.15
    private const val DAILY_XP_MIN = 30

    data class Completed(val mission: Mission, val xp: Int)

    data class Current(val mission: Mission, val progress: Long, val dailyIndex: Int?)

    fun fresh(uuid: String): PlayerMissions = PlayerMissions(uuid, 0, 0, "", emptyList())

    fun trailDone(state: PlayerMissions): Boolean = state.trailIndex >= MissionTrail.size

    fun apply(state: PlayerMissions, signal: MissionSignal): Pair<PlayerMissions, List<Completed>> =
        if (trailDone(state)) applyDailies(state, signal) else applyTrail(state, signal)

    fun needsReset(state: PlayerMissions, today: LocalDate): Boolean =
        trailDone(state) && state.dailyDate != today.toString()

    fun rollDailies(today: LocalDate, level: Int, random: Random): List<DailyMission> {
        val xp = max(DAILY_XP_MIN, (DAILY_XP_SHARE * LevelCurve.toNext(level)).roundToInt())
        val picked = mutableListOf<Mission>()
        for (mission in DailyPool.all.shuffled(random)) {
            if (picked.size == DailyPool.DAILY_COUNT) break
            if (picked.none { DailyPool.group(it.id) == DailyPool.group(mission.id) }) picked += mission
        }
        return picked.map { DailyMission(it.id, 0, xp, false) }
    }

    fun current(state: PlayerMissions): Current? {
        MissionTrail.at(state.trailIndex)?.let { return Current(it, state.trailProgress, null) }
        state.dailies.forEachIndexed { index, daily ->
            if (!daily.done) DailyPool.byId(daily.id)?.let { return Current(it, daily.progress, index) }
        }
        return null
    }

    fun skip(state: PlayerMissions): PlayerMissions {
        if (!trailDone(state)) return state.copy(trailIndex = state.trailIndex + 1, trailProgress = 0)
        val index = state.dailies.indexOfFirst { !it.done }
        if (index < 0) return state
        return state.copy(dailies = state.dailies.mapIndexed { i, daily -> if (i == index) daily.copy(done = true) else daily })
    }

    fun credit(objective: MissionObjective, signal: MissionSignal): Long = when (objective) {
        is MissionObjective.CollectDebris ->
            if (signal is MissionSignal.Debris && (signal.byHook || !objective.hookOnly)) 1 else 0
        MissionObjective.BuildGenerator -> when (signal) {
            MissionSignal.GeneratorFormed, is MissionSignal.GeneratorBlock -> 1
            is MissionSignal.State -> if (signal.hasGenerator) 1 else 0
            else -> 0
        }
        is MissionObjective.BreakGeneratorBlocks ->
            if (signal is MissionSignal.GeneratorBlock && (objective.material == null || objective.material == signal.material)) 1 else 0
        is MissionObjective.Craft ->
            if (signal is MissionSignal.Crafted && signal.recipeId == objective.recipeId) 1 else 0
        is MissionObjective.CraftAny -> if (signal is MissionSignal.Crafted) 1 else 0
        is MissionObjective.Smelt -> if (signal is MissionSignal.Smelted) max(0, signal.amount).toLong() else 0
        is MissionObjective.ReachLevel ->
            if (signal is MissionSignal.State && signal.level >= objective.level) 1 else 0
        is MissionObjective.Sell -> if (signal is MissionSignal.Sold) max(0, signal.value) else 0
        is MissionObjective.IslandUpgrade ->
            if (signal is MissionSignal.State && (signal.upgrades?.get(objective.upgrade) ?: 0) >= objective.level) 1 else 0
        is MissionObjective.KillMobs -> if (signal == MissionSignal.MobKilled) 1 else 0
    }

    private fun applyTrail(state: PlayerMissions, signal: MissionSignal): Pair<PlayerMissions, List<Completed>> {
        var current = state
        val completed = mutableListOf<Completed>()
        while (true) {
            val mission = MissionTrail.at(current.trailIndex) ?: break
            val gained = credit(mission.objective, signal)
            if (gained <= 0) break
            val progress = current.trailProgress + gained
            if (progress < mission.objective.goal) {
                current = current.copy(trailProgress = progress)
                break
            }
            completed += Completed(mission, mission.xp)
            current = current.copy(trailIndex = current.trailIndex + 1, trailProgress = 0)
            if (signal !is MissionSignal.State) break
        }
        return current to completed
    }

    private fun applyDailies(state: PlayerMissions, signal: MissionSignal): Pair<PlayerMissions, List<Completed>> {
        state.dailies.forEachIndexed { index, daily ->
            if (daily.done) return@forEachIndexed
            val mission = DailyPool.byId(daily.id) ?: return@forEachIndexed
            val gained = credit(mission.objective, signal)
            if (gained <= 0) return@forEachIndexed
            val progress = minOf(daily.progress + gained, mission.objective.goal)
            val done = progress >= mission.objective.goal
            val updated = daily.copy(progress = progress, done = done)
            val next = state.copy(dailies = state.dailies.mapIndexed { i, it -> if (i == index) updated else it })
            return next to if (done) listOf(Completed(mission, daily.xp)) else emptyList()
        }
        return state to emptyList()
    }
}
