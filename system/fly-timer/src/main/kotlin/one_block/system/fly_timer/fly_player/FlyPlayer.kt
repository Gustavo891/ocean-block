package one_block.system.fly_timer.fly_player

import java.time.Instant
import java.util.UUID

data class FlyPlayer(
    val uuid: UUID,
    val name: String,
    private var remainingSeconds: Long,
) {

    var flyingSince: Instant? = null

    fun addSeconds(seconds: Long) {
        remainingSeconds += seconds
    }

    fun startFlying() {
        if  (flyingSince != null) return
        flyingSince = Instant.now()
    }

    fun stopFlying() {
        val start = flyingSince ?: return

        val used = Instant.now().epochSecond - start.epochSecond
        remainingSeconds -= used

        if (remainingSeconds < 0) remainingSeconds = 0
        flyingSince = null
    }

    fun getRemainingSeconds(): Long {
        val start = flyingSince ?: return remainingSeconds
        val used = Instant.now().epochSecond - start.epochSecond

        val current = remainingSeconds - used
        return if (current < 0) 0 else current
    }

    fun hasTime() = getRemainingSeconds() > 0
}
