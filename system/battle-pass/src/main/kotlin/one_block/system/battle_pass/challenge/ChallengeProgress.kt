package one_block.system.battle_pass.challenge

data class ChallengeProgress(
    val challengeId: String,
    var current: Int = 0,
    var completed: Boolean = false,
    var xpClaimed: Boolean = false,
)
