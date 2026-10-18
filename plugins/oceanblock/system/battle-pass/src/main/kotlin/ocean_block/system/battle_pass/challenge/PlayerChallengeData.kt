package ocean_block.system.battle_pass.challenge

import java.util.UUID

data class PlayerChallengeData(
    val playerUuid: UUID,
    val progresses: MutableMap<String, ChallengeProgress> = mutableMapOf()
)
