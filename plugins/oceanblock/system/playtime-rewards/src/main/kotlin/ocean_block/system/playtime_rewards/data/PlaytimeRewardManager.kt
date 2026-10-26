package ocean_block.system.playtime_rewards.data

import ocean_block.libs.instance.Instance
import ocean_block.libs.instance.ScheduledExecutor
import java.util.UUID
import java.util.concurrent.TimeUnit

object PlaytimeRewardManager {

    private const val FLUSH_SECONDS = 30L

    private val lock = Any()
    private val claimed = mutableMapOf<UUID, Set<String>>()
    private val dirty = mutableSetOf<UUID>()
    private val repository by lazy { PlaytimeRewardRepository() }

    fun onInit() {
        val loaded = runCatching { repository.toList() }
            .onFailure { Instance.logger.severe("[recompensas] Falha ao carregar resgates: ${it.message}") }
            .getOrDefault(emptyList())
        synchronized(lock) {
            loaded.forEach { entry ->
                runCatching { UUID.fromString(entry.uuid) }.onSuccess { claimed[it] = entry.claimed.toSet() }
            }
        }
        ScheduledExecutor.scheduleAtFixedRate({ flush() }, FLUSH_SECONDS, FLUSH_SECONDS, TimeUnit.SECONDS)
    }

    fun onDisable() {
        flush()
    }

    fun claimed(uuid: UUID): Set<String> = synchronized(lock) { claimed[uuid] ?: emptySet() }

    fun setClaimed(uuid: UUID, ids: Set<String>) = synchronized(lock) {
        claimed[uuid] = ids
        dirty += uuid
    }

    private fun flush() {
        val (ids, copies) = synchronized(lock) {
            val ids = dirty.toSet()
            ids to ids.map { PlaytimeRewardData(it.toString(), (claimed[it] ?: emptySet()).toList()) }
        }
        if (copies.isEmpty()) return
        val saved = runCatching { repository.updateAll(copies) }
        if (saved.isFailure) {
            Instance.logger.severe("[recompensas] Falha ao salvar ${copies.size} resgate(s): ${saved.exceptionOrNull()?.message}. Seguem marcados.")
            return
        }
        synchronized(lock) { dirty.removeAll(ids) }
    }
}
