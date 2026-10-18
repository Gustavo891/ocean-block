package one_block.system.coin_flip.coin_flip

import java.time.Duration
import java.time.Instant
import java.util.*

data class CoinFlip(
    val uuid: UUID,
    val creator: UUID,
    val creatorName: String,
    val amount: Double,

    var opponent: UUID? = null,
    var startInstant: Instant? = null,
) {

    fun start() {
        startInstant = Instant.now()
    }

    fun shouldFinish(now: Instant, delay: Duration): Boolean {
        val start = startInstant ?: return false
        return Duration.between(start, now) >= delay
    }
}
