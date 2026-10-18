package one_block.system.combatlog.combat_player

import java.time.Duration
import java.time.Instant
import java.util.*

data class CombatPlayer(
    val uuid: UUID,
) {

    private var timestamp: Instant = Instant.now()
    private var lastAttackerName: String? = null

    val isExpired: Boolean get() = Duration.between(timestamp, Instant.now()).seconds >= 15

    val secondsRemaining: Long
        get() {
            val elapsed = Duration.between(timestamp, Instant.now()).seconds
            return maxOf(0, 15 - elapsed)
        }

    fun renewCombat(attackerName: String) {
        timestamp = Instant.now()
        lastAttackerName = attackerName
    }
}
