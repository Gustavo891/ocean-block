package ocean_block.system.research.data

import ocean_block.libs.instance.Instance
import ocean_block.libs.instance.ScheduledExecutor
import ocean_block.system.research.model.IslandResearch
import ocean_block.system.research.model.RunningResearch
import java.util.UUID
import java.util.concurrent.TimeUnit

object ResearchManager {

    private const val FLUSH_SECONDS = 30L

    private val lock = Any()
    private val states = mutableMapOf<UUID, IslandResearch>()
    private val dirty = mutableSetOf<UUID>()
    private val repository by lazy { ResearchRepository() }

    fun onInit() {
        val loaded = runCatching { repository.toList() }
            .onFailure { Instance.logger.severe("[pesquisa] Falha ao carregar pesquisas: ${it.message}") }
            .getOrDefault(emptyList())
        synchronized(lock) { loaded.forEach { states[it.islandUuid] = it } }
        ScheduledExecutor.scheduleAtFixedRate({ flush() }, FLUSH_SECONDS, FLUSH_SECONDS, TimeUnit.SECONDS)
    }

    fun onDisable() {
        flush()
    }

    fun <T> edit(islandUuid: UUID, change: (IslandResearch) -> T): T = synchronized(lock) {
        val state = states.getOrPut(islandUuid) { IslandResearch(islandUuid) }
        change(state).also { dirty += islandUuid }
    }

    fun <T> read(islandUuid: UUID, view: (IslandResearch) -> T): T = synchronized(lock) {
        view(states[islandUuid] ?: IslandResearch(islandUuid))
    }

    fun completed(islandUuid: UUID): Set<String> = read(islandUuid) { it.completed.toSet() }

    fun withRunning(): List<UUID> = synchronized(lock) { states.values.filter { it.running.isNotEmpty() }.map { it.islandUuid } }

    fun saveNow(islandUuid: UUID) {
        val copy = synchronized(lock) { states[islandUuid]?.let(::copyOf) } ?: return
        ScheduledExecutor.execute {
            runCatching { repository.update(copy) }
                .onFailure {
                    Instance.logger.severe("[pesquisa] Falha ao salvar a pesquisa de $islandUuid: ${it.message}")
                    synchronized(lock) { dirty += islandUuid }
                }
        }
    }

    private fun flush() {
        val (ids, copies) = synchronized(lock) {
            val ids = dirty.toSet()
            ids to ids.mapNotNull { states[it]?.let(::copyOf) }
        }
        if (copies.isEmpty()) return
        val saved = runCatching { repository.updateAll(copies) }
        if (saved.isFailure) {
            Instance.logger.severe("[pesquisa] Falha ao salvar ${copies.size} raft(s): ${saved.exceptionOrNull()?.message}. Seguem marcados.")
            return
        }
        synchronized(lock) { dirty.removeAll(ids) }
    }

    private fun copyOf(state: IslandResearch): IslandResearch = IslandResearch(
        islandUuid = state.islandUuid,
        delivered = state.delivered.mapValues { it.value.toMutableMap() }.toMutableMap(),
        completed = state.completed.toMutableSet(),
        queue = state.queue.toMutableList(),
        running = state.running.map { RunningResearch(it.id, it.startedAt, it.durationMs) }.toMutableList(),
    )
}
