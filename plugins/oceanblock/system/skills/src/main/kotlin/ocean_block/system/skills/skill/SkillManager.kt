package ocean_block.system.skills.skill

import ocean_block.libs.instance.Instance
import ocean_block.libs.instance.ScheduledExecutor
import java.util.UUID
import java.util.concurrent.TimeUnit

object SkillManager {

    private const val FLUSH_INTERVAL_SECONDS = 30L

    private val dataByUuid = mutableMapOf<UUID, SkillData>()
    private val dirtyIds = mutableSetOf<UUID>()

    private val repository = SkillRepository()

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
        val data = repository.find(uuid) ?: SkillData(uuid, name.lowercase())
        dataByUuid[uuid] = data

        if (data.migrateLegacy()) markDirty(uuid)
    }

    fun unload(uuid: UUID) {
        val data = dataByUuid.remove(uuid) ?: return
        if (dirtyIds.remove(uuid)) repository.update(data)
    }

    fun get(uuid: UUID): SkillData? = dataByUuid[uuid]

    fun markDirty(uuid: UUID) {
        dirtyIds.add(uuid)
    }

    fun saveDirty() {
        if (dirtyIds.isEmpty()) return

        val toSave = dirtyIds.mapNotNull { dataByUuid[it] }
        dirtyIds.clear()

        repository.updateAll(toSave)
        Instance.logger.info("[skills] Salvos ${toSave.size} registro(s) de skill.")
    }

    fun toList(): List<SkillData> = dataByUuid.values.toList()
}
