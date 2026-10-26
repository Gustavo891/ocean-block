package ocean_block.system.playtime_rewards.rules

import ocean_block.system.playtime_rewards.model.Milestone

sealed interface MilestoneStatus {
    data object Claimed : MilestoneStatus
    data object Available : MilestoneStatus
    data class Locked(val missingSeconds: Long) : MilestoneStatus
}

sealed interface ClaimResult {
    data class Claimed(val claimed: Set<String>) : ClaimResult
    data object AlreadyClaimed : ClaimResult
    data class NotReached(val missingSeconds: Long) : ClaimResult
}

object MilestoneRules {

    fun status(milestone: Milestone, seconds: Long, claimed: Set<String>): MilestoneStatus = when {
        milestone.id in claimed -> MilestoneStatus.Claimed
        seconds >= milestone.seconds -> MilestoneStatus.Available
        else -> MilestoneStatus.Locked(milestone.seconds - seconds)
    }

    fun reached(ladder: List<Milestone>, before: Long, now: Long, claimed: Set<String>): List<Milestone> =
        ladder.filter { it.seconds in (before + 1)..now && it.id !in claimed }

    fun available(ladder: List<Milestone>, seconds: Long, claimed: Set<String>): List<Milestone> =
        ladder.filter { status(it, seconds, claimed) == MilestoneStatus.Available }

    fun claim(milestone: Milestone, seconds: Long, claimed: Set<String>): ClaimResult =
        when (val current = status(milestone, seconds, claimed)) {
            MilestoneStatus.Claimed -> ClaimResult.AlreadyClaimed
            MilestoneStatus.Available -> ClaimResult.Claimed(claimed + milestone.id)
            is MilestoneStatus.Locked -> ClaimResult.NotReached(current.missingSeconds)
        }

    fun startPage(ladder: List<Milestone>, claimed: Set<String>, perPage: Int): Int {
        if (ladder.isEmpty()) return 0
        val index = ladder.indexOfFirst { it.id !in claimed }.takeIf { it >= 0 } ?: ladder.lastIndex
        return index / perPage
    }

    fun progress(milestone: Milestone, seconds: Long): Int =
        (seconds.coerceIn(0, milestone.seconds) * 100 / milestone.seconds).toInt()
}
