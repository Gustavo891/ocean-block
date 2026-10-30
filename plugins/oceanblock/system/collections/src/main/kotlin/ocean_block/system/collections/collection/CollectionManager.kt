package ocean_block.system.collections.collection

import ocean_block.libs.instance.Instance
import ocean_block.libs.instance.ScheduledExecutor
import java.util.UUID
import java.util.concurrent.TimeUnit

object CollectionManager {

    private const val FLUSH_INTERVAL_SECONDS = 30L

    private val dataByUuid = mutableMapOf<UUID, CollectionData>()
    private val dirtyIds = mutableSetOf<UUID>()

    private val repository = CollectionRepository()

    fun onInit() {
        ScheduledExecutor.scheduleAtFixedRate(
            { saveDirty() },
            FLUSH_INTERVAL_SECONDS,
            FLUSH_INTERVAL_SECONDS,
            TimeUnit.SECONDS,
        )
    }

    fun onDisable() = saveDirty()

    fun load(uuid: UUID, name: String) {
        val data = repository.find(uuid) ?: CollectionData(uuid, name.lowercase())
        dataByUuid[uuid] = data
    }

    fun unload(uuid: UUID) {
        val data = dataByUuid.remove(uuid) ?: return
        if (dirtyIds.remove(uuid)) repository.update(data)
    }

    fun get(uuid: UUID): CollectionData? = dataByUuid[uuid]

    fun reset(uuid: UUID, name: String) {
        dataByUuid[uuid] = CollectionData(uuid, name.lowercase())
        markDirty(uuid)
    }

    fun markDirty(uuid: UUID) {
        dirtyIds.add(uuid)
    }

    fun saveDirty() {
        if (dirtyIds.isEmpty()) return

        val toSave = dirtyIds.mapNotNull { dataByUuid[it] }
        dirtyIds.clear()

        repository.updateAll(toSave)
        Instance.logger.info("[collections] Salvos ${toSave.size} registro(s) de coleção.")
    }

    fun toList(): List<CollectionData> = dataByUuid.values.toList()
}
