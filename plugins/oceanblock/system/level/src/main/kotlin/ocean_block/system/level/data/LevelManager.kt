package ocean_block.system.level.data

import ocean_block.libs.instance.Instance
import ocean_block.libs.instance.ScheduledExecutor
import ocean_block.system.level.curve.LevelCurve
import java.util.UUID
import java.util.concurrent.TimeUnit

object LevelManager {

    private const val FLUSH_SECONDS = 30L

    private val lock = Any()
    private val xp = mutableMapOf<UUID, Long>()
    private val dirty = mutableSetOf<UUID>()
    private val repository by lazy { LevelRepository() }

    fun onInit() {
        val loaded = runCatching { repository.toList() }
            .onFailure { Instance.logger.severe("[nivel] Falha ao carregar níveis: ${it.message}") }
            .getOrDefault(emptyList())
        synchronized(lock) {
            loaded.forEach { entry -> runCatching { UUID.fromString(entry.uuid) }.onSuccess { xp[it] = entry.xp } }
        }
        ScheduledExecutor.scheduleAtFixedRate({ flush() }, FLUSH_SECONDS, FLUSH_SECONDS, TimeUnit.SECONDS)
    }

    fun onDisable() {
        flush()
    }

    fun xp(uuid: UUID): Long = synchronized(lock) { xp[uuid] ?: 0L }

    fun setXp(uuid: UUID, value: Long): Long = synchronized(lock) {
        val capped = value.coerceIn(0L, LevelCurve.totalFor(LevelCurve.MAX_LEVEL))
        xp[uuid] = capped
        dirty += uuid
        capped
    }

    private fun flush() {
        val (ids, copies) = synchronized(lock) {
            val ids = dirty.toSet()
            ids to ids.map { PlayerLevel(it.toString(), xp[it] ?: 0L) }
        }
        if (copies.isEmpty()) return
        val saved = runCatching { repository.updateAll(copies) }
        if (saved.isFailure) {
            Instance.logger.severe("[nivel] Falha ao salvar ${copies.size} nível(is): ${saved.exceptionOrNull()?.message}. Seguem marcados.")
            return
        }
        synchronized(lock) { dirty.removeAll(ids) }
    }
}
