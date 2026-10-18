package one_block.system.sell.manager

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

object SellCooldown {

    private val expiries = ConcurrentHashMap<UUID, Long>()

    fun remainingMillis(uuid: UUID): Long {
        val expiry = expiries[uuid] ?: return 0L
        val remaining = expiry - System.currentTimeMillis()
        if (remaining <= 0L) {
            expiries.remove(uuid)
            return 0L
        }
        return remaining
    }

    fun remainingSeconds(uuid: UUID): Int = ceil(remainingMillis(uuid) / 1000.0).toInt()

    fun stamp(uuid: UUID, durationMillis: Long) {
        expiries[uuid] = System.currentTimeMillis() + durationMillis
    }

    fun clear(uuid: UUID) {
        expiries.remove(uuid)
    }
}
