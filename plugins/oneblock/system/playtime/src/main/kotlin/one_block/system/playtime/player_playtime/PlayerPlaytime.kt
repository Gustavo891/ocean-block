package one_block.system.playtime.player_playtime

import java.time.Duration
import java.time.Instant
import java.util.*

data class PlayerPlaytime(
    val uuid: UUID,
    val name: String,
    var playtime: Long,
) {
    private var startTime = Instant.now()

    fun updateElapsedSeconds() {
        val now = Instant.now()

        playtime += Duration.between(startTime, now).seconds
        startTime = now
    }

    fun getElapsedSeconds() = playtime + Duration.between(startTime, Instant.now()).seconds
}
