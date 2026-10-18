package ocean_block.system.custom_items.cooldown

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object CooldownStore {

    private val map = ConcurrentHashMap<UUID, MutableMap<String, Long>>()

    fun isReady(uuid: UUID, id: String): Boolean = remaining(uuid, id) <= 0L

    fun remaining(uuid: UUID, id: String): Long {
        val expiry = map[uuid]?.get(id) ?: return 0L
        return (expiry - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun stamp(uuid: UUID, id: String, durationMillis: Long) {
        if (durationMillis <= 0L) return
        map.getOrPut(uuid) { ConcurrentHashMap() }[id] = System.currentTimeMillis() + durationMillis
    }

    fun clear(uuid: UUID) {
        map.remove(uuid)
    }
}
