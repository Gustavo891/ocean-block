package ocean_block.system.missions.data

import ocean_block.libs.instance.Instance
import ocean_block.libs.instance.ScheduledExecutor
import ocean_block.system.missions.model.PlayerMissions
import ocean_block.system.missions.rules.MissionRules
import java.util.UUID
import java.util.concurrent.TimeUnit

object MissionManager {

    private const val FLUSH_SECONDS = 30L

    private val lock = Any()
    private val states = mutableMapOf<UUID, PlayerMissions>()
    private val dirty = mutableSetOf<UUID>()
    private val repository by lazy { MissionRepository() }

    fun onInit() {
        val loaded = runCatching { repository.toList() }
            .onFailure { Instance.logger.severe("[missao] Falha ao carregar missões: ${it.message}") }
            .getOrDefault(emptyList())
        synchronized(lock) {
            loaded.forEach { entry -> runCatching { UUID.fromString(entry.uuid) }.onSuccess { states[it] = entry } }
        }
        ScheduledExecutor.scheduleAtFixedRate({ flush() }, FLUSH_SECONDS, FLUSH_SECONDS, TimeUnit.SECONDS)
    }

    fun onDisable() {
        flush()
    }

    fun get(uuid: UUID): PlayerMissions = synchronized(lock) { states[uuid] ?: MissionRules.fresh(uuid.toString()) }

    fun set(uuid: UUID, state: PlayerMissions) = synchronized(lock) {
        states[uuid] = state.copy(uuid = uuid.toString())
        dirty += uuid
    }

    private fun flush() {
        val (ids, copies) = synchronized(lock) {
            val ids = dirty.toSet()
            ids to ids.mapNotNull { states[it] }
        }
        if (copies.isEmpty()) return
        val saved = runCatching { repository.updateAll(copies) }
        if (saved.isFailure) {
            Instance.logger.severe("[missao] Falha ao salvar ${copies.size} registro(s) de missão: ${saved.exceptionOrNull()?.message}. Seguem marcados.")
            return
        }
        synchronized(lock) { dirty.removeAll(ids) }
    }
}
