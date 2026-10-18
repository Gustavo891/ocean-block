package ocean_block.system.battle_pass.player

import java.util.UUID

data class PlayerPassData(
    val playerUuid: UUID,
    var xp: Int = 0,
    val claimedFree: MutableSet<Int> = mutableSetOf(),
    val claimedPremium: MutableSet<Int> = mutableSetOf(),
    var activeChallengeId: String? = null,
)
