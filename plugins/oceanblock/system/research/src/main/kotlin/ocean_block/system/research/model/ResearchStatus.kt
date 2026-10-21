package ocean_block.system.research.model

sealed interface ResearchStatus {
    data class Locked(val missing: List<String>) : ResearchStatus
    data object Available : ResearchStatus
    data object Queued : ResearchStatus
    data class Running(val remainingMs: Long) : ResearchStatus
    data object Done : ResearchStatus
}
