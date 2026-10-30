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

    fun apply(state: PlayerMissions, signal: MissionSignal): Pair<PlayerMissions, List<Completed>> {
        val (afterTrail, trail) = applyTrail(state, signal)
        val (afterDailies, dailies) = applyDailies(afterTrail, signal)
        return afterDailies to trail + dailies
    }

    fun needsReset(state: PlayerMissions, today: LocalDate): Boolean = state.dailyDate != today.toString()

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
        val index = activeIndex(state)
        if (index >= 0) {
            val daily = state.dailies[index]
            DailyPool.byId(daily.id)?.let { return Current(it, daily.progress, index) }
        }
        return MissionTrail.at(state.trailIndex)?.let { Current(it, state.trailProgress, null) }
    }

    fun choose(state: PlayerMissions, id: String): PlayerMissions? {
        if (state.dailies.none { it.id == id && !it.done }) return null
        return state.copy(activeDaily = id)
    }

    fun needsChoice(state: PlayerMissions): Boolean =
        activeIndex(state) < 0 && state.dailies.any { !it.done }

    fun skip(state: PlayerMissions): PlayerMissions {
        val index = activeIndex(state)
        if (index < 0) {
            return if (trailDone(state)) state else state.copy(trailIndex = state.trailIndex + 1, trailProgress = 0)
        }
        return state.copy(
            dailies = state.dailies.mapIndexed { i, daily -> if (i == index) daily.copy(done = true) else daily },
            activeDaily = null,
        )
    }

    private fun activeIndex(state: PlayerMissions): Int =
        state.dailies.indexOfFirst { it.id == state.activeDaily && !it.done }

    fun credit(objective: MissionObjective, signal: MissionSignal): Long = when (objective) {
        is MissionObjective.CollectDebris ->
            if (signal is MissionSignal.Debris && (signal.byHook || !objective.hookOnly) && (objective.type == null || objective.type == signal.type)) 1 else 0
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
        val index = activeIndex(state)
        if (index < 0) return state to emptyList()
        val daily = state.dailies[index]
        val mission = DailyPool.byId(daily.id) ?: return state to emptyList()
        val gained = credit(mission.objective, signal)
        if (gained <= 0) return state to emptyList()
        val progress = minOf(daily.progress + gained, mission.objective.goal)
        val done = progress >= mission.objective.goal
        val updated = daily.copy(progress = progress, done = done)
        val next = state.copy(
            dailies = state.dailies.mapIndexed { i, it -> if (i == index) updated else it },
            activeDaily = if (done) null else state.activeDaily,
        )
        return next to if (done) listOf(Completed(mission, daily.xp)) else emptyList()
    }
}
