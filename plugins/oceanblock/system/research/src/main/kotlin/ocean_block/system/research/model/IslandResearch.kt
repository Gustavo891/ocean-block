package ocean_block.system.research.model

import java.util.UUID

data class RunningResearch(var id: String = "", var startedAt: Long = 0, var durationMs: Long = 0) {
    val endsAt: Long get() = startedAt + durationMs
}

data class IslandResearch(
    var islandUuid: UUID = UUID(0, 0),
    val delivered: MutableMap<String, MutableMap<String, Int>> = mutableMapOf(),
    val completed: MutableSet<String> = mutableSetOf(),
    val queue: MutableList<String> = mutableListOf(),
    val running: MutableList<RunningResearch> = mutableListOf(),
)
