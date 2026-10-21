package ocean_block.system.research.rules

import ocean_block.system.research.model.IslandResearch
import ocean_block.system.research.model.ResearchDefinition
import ocean_block.system.research.model.ResearchStatus
import ocean_block.system.research.model.RunningResearch

object ResearchRules {

    private const val BASE_SLOTS = 1

    fun slots(state: IslandResearch, tree: Map<String, ResearchDefinition>): Int =
        BASE_SLOTS + state.completed.sumOf { tree[it]?.extraSlots ?: 0 }

    fun status(def: ResearchDefinition, state: IslandResearch, now: Long): ResearchStatus {
        if (def.id in state.completed) return ResearchStatus.Done
        state.running.firstOrNull { it.id == def.id }?.let { return ResearchStatus.Running((it.endsAt - now).coerceAtLeast(0)) }
        if (def.id in state.queue) return ResearchStatus.Queued
        val missing = def.requires.filter { it !in state.completed }
        return if (missing.isNotEmpty()) ResearchStatus.Locked(missing) else ResearchStatus.Available
    }

    fun missing(def: ResearchDefinition, state: IslandResearch): Map<String, Int> {
        val delivered = state.delivered[def.id].orEmpty()
        return def.cost.mapNotNull { cost ->
            val left = cost.amount - (delivered[cost.key] ?: 0)
            if (left > 0) cost.key to left else null
        }.toMap()
    }

    fun deliver(
        def: ResearchDefinition,
        state: IslandResearch,
        available: Map<String, Int>,
        now: Long,
        tree: Map<String, ResearchDefinition>,
    ): Map<String, Int> {
        if (status(def, state, now) != ResearchStatus.Available) return emptyMap()
        val taken = missing(def, state).mapNotNull { (key, left) ->
            val amount = minOf(left, available[key] ?: 0)
            if (amount > 0) key to amount else null
        }.toMap()
        if (taken.isEmpty()) return taken

        val delivered = state.delivered.getOrPut(def.id) { mutableMapOf() }
        taken.forEach { (key, amount) -> delivered[key] = (delivered[key] ?: 0) + amount }
        if (missing(def, state).isEmpty()) {
            state.queue += def.id
            startQueued(state, now, tree)
        }
        return taken
    }

    fun startQueued(state: IslandResearch, now: Long, tree: Map<String, ResearchDefinition>) {
        while (state.running.size < slots(state, tree) && state.queue.isNotEmpty()) {
            val id = state.queue.removeAt(0)
            val def = tree[id] ?: continue
            state.running += RunningResearch(id, now, def.durationMs)
        }
    }

    fun completeDue(state: IslandResearch, now: Long, tree: Map<String, ResearchDefinition>): List<String> {
        val done = mutableListOf<String>()
        while (true) {
            val next = state.running.filter { it.endsAt <= now }.minByOrNull { it.endsAt } ?: break
            state.running.remove(next)
            state.completed += next.id
            state.delivered.remove(next.id)
            done += next.id
            startQueued(state, next.endsAt, tree)
        }
        return done
    }
}
