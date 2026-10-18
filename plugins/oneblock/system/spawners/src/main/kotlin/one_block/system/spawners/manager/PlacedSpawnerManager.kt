package one_block.system.spawners.manager

import one_block.libs.coordinate.SimpleLocation
import one_block.libs.instance.Instance
import one_block.libs.instance.ScheduledExecutor
import one_block.system.spawners.repo.PlacedSpawnerRepository
import one_block.system.spawners.models.PlacedSpawner
import one_block.system.spawners.rules.PlacementRules
import one_block.system.spawners.rules.SpawnRules
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object PlacedSpawnerManager {

    const val SAVE_INTERVAL_SECONDS = 10L
    private const val SAVE_INTERVAL_TICKS = SAVE_INTERVAL_SECONDS * 20L
    private const val INIT_DELAY_TICKS = 40L
    private val byLocation = ConcurrentHashMap<SimpleLocation, PlacedSpawner>()
    private val byUuid = ConcurrentHashMap<UUID, PlacedSpawner>()
    private val dirtyIds = mutableSetOf<UUID>()

    private val validated = mutableSetOf<UUID>()
    private val warnedUnknownType = mutableSetOf<UUID>()
    private val ticksSinceSpawn = ConcurrentHashMap<UUID, Long>()
    private val repository by lazy { PlacedSpawnerRepository() }

    fun onInit() {
        Bukkit.getScheduler().runTaskLater(Instance, Runnable {
            repository.toList().forEach { index(it) }
            Instance.logger.info("[spawners] ${byUuid.size} spawner(s) carregado(s).")

            Bukkit.getScheduler().runTaskTimer(Instance, Runnable { processTick() }, 1L, 1L)
            Bukkit.getScheduler().runTaskTimer(
                Instance,
                Runnable { flushDirty() },
                SAVE_INTERVAL_TICKS,
                SAVE_INTERVAL_TICKS,
            )
        }, INIT_DELAY_TICKS)
    }

    fun onDisable() {
        writeAll(takeDirtySnapshot())
    }

    fun keyOf(block: Block): SimpleLocation = SimpleLocation.fromBukkit(block.location)

    fun get(location: SimpleLocation): PlacedSpawner? = byLocation[location]

    fun get(block: Block): PlacedSpawner? = byLocation[keyOf(block)]

    fun get(uuid: UUID): PlacedSpawner? = byUuid[uuid]

    fun all(): List<PlacedSpawner> = byUuid.values.toList()

    fun hasNeighborWithin(location: SimpleLocation): Boolean =
        byUuid.values.any { other ->
            other.location.world == location.world &&
                    PlacementRules.isTooClose(
                        location.x, location.y, location.z,
                        other.location.x, other.location.y, other.location.z,
                    )
        }

    fun register(placed: PlacedSpawner) {
        index(placed)
        saveNow(placed)
    }

    fun unregister(location: SimpleLocation): PlacedSpawner? {
        val removed = byLocation.remove(location) ?: return null
        byUuid.remove(removed.uuid)
        validated.remove(removed.uuid)
        warnedUnknownType.remove(removed.uuid)
        ticksSinceSpawn.remove(removed.uuid)
        synchronized(dirtyIds) { dirtyIds.remove(removed.uuid) }

        SpawnerEntity.remove(removed)
        SpawnerDisplay.remove(removed)
        repository.remove(removed.uuid)
        return removed
    }

    fun isRegistered(placed: PlacedSpawner): Boolean = byUuid[placed.uuid] === placed

    fun markDirty(placed: PlacedSpawner) {
        if (!isRegistered(placed)) return
        synchronized(dirtyIds) { dirtyIds.add(placed.uuid) }
    }

    fun saveNow(placed: PlacedSpawner) {
        if (!isRegistered(placed)) return
        synchronized(dirtyIds) { dirtyIds.remove(placed.uuid) }
        repository.update(placed)
    }

    private fun index(placed: PlacedSpawner) {
        byLocation[placed.location] = placed
        byUuid[placed.uuid] = placed
    }

    private fun takeDirtySnapshot(): List<PlacedSpawner> {
        val ids = synchronized(dirtyIds) {
            if (dirtyIds.isEmpty()) return emptyList()
            val copy = dirtyIds.toList()
            dirtyIds.clear()
            copy
        }

        return ids.mapNotNull { byUuid[it] }.map { it.copy() }
    }

    private fun writeAll(spawners: List<PlacedSpawner>) {
        if (spawners.isEmpty()) return
        runCatching { repository.updateAll(spawners) }
            .onFailure { Instance.logger.warning("[spawners] flush falhou: ${it.message}") }
    }

    private fun flushDirty() {
        val snapshot = takeDirtySnapshot()
        if (snapshot.isEmpty()) return

        ScheduledExecutor.execute { writeAll(snapshot) }
    }

    private fun processTick() {
        byUuid.values.toList().forEach { placed ->
            val type = placed.type() ?: run {
                warnUnknownType(placed)
                return@forEach
            }
            if (!isChunkLoaded(placed)) return@forEach
            if (!isStillValid(placed)) return@forEach

            if (!placed.enabled) {
                SpawnerEntity.remove(placed)
                SpawnerDisplay.reconcile(placed, type)
                return@forEach
            }

            val elapsed = (ticksSinceSpawn[placed.uuid] ?: 0L) + 1L
            if (elapsed >= type.spawnIntervalTicks) {
                ticksSinceSpawn[placed.uuid] = 0L
                if (SpawnRules.shouldSpawn(placed.enabled, placed.liveMobs, type.maxLiveMobs)) {
                    placed.liveMobs = SpawnRules.nextLiveMobs(placed.liveMobs, placed.stackSize, type.maxLiveMobs)
                    markDirty(placed)
                }
            } else {
                ticksSinceSpawn[placed.uuid] = elapsed
            }

            runCatching {
                SpawnerEntity.reconcile(placed, type)
                SpawnerDisplay.reconcile(placed, type)
            }.onFailure {
                Instance.logger.warning("[spawners] reconciliação de ${placed.uuid} falhou: ${it.message}")
            }
        }
    }

    private fun warnUnknownType(placed: PlacedSpawner) {
        if (!warnedUnknownType.add(placed.uuid)) return
        Instance.logger.warning(
            "[spawners] spawner ${placed.uuid} em ${placed.location} tem typeId " +
                    "'${placed.typeId}' sem SpawnerType correspondente."
        )
    }

    private fun isChunkLoaded(placed: PlacedSpawner): Boolean {
        val bukkit: Location = placed.location.toBukkit() ?: return false
        val world = bukkit.world ?: return false
        return world.isChunkLoaded(bukkit.blockX shr 4, bukkit.blockZ shr 4)
    }

    private fun isStillValid(placed: PlacedSpawner): Boolean {
        if (placed.uuid in validated) return true

        val bukkit = placed.location.toBukkit() ?: return false
        if (bukkit.block.type != Material.SPAWNER) {
            Instance.logger.warning(
                "[spawners] spawner sumiu em ${placed.location}; registro removido."
            )
            unregister(placed.location)
            return false
        }

        validated.add(placed.uuid)
        return true
    }
}
