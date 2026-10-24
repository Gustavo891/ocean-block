package ocean_block.system.custom_items.block

import ocean_block.libs.coordinate.SimpleLocation
import ocean_block.libs.instance.Instance
import ocean_block.libs.instance.ScheduledExecutor
import ocean_block.system.islands.island.IslandManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Block
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object PlacedBlockManager {

    const val SAVE_INTERVAL_SECONDS = 10L

    private const val SAVE_INTERVAL_TICKS = SAVE_INTERVAL_SECONDS * 20L

    private const val INIT_DELAY_TICKS = 40L

    private val byLocation = ConcurrentHashMap<SimpleLocation, PlacedBlock>()
    private val byUuid = ConcurrentHashMap<UUID, PlacedBlock>()
    private val dirtyIds = mutableSetOf<UUID>()

    private val validated = mutableSetOf<UUID>()

    private val warnedUnknownBlockId = mutableSetOf<UUID>()

    private val repository by lazy { PlacedBlockRepository() }

    private var tickCounter = 0L

    fun onInit() {
        Bukkit.getScheduler().runTaskLater(Instance, Runnable {
            repository.toList().forEach { index(it) }
            Instance.logger.info("[custom-items] ${byUuid.size} bloco(s) custom carregado(s).")

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

    fun get(location: SimpleLocation): PlacedBlock? = byLocation[location]

    fun get(block: Block): PlacedBlock? = byLocation[keyOf(block)]

    fun get(uuid: UUID): PlacedBlock? = byUuid[uuid]

    fun all(): List<PlacedBlock> = byUuid.values.toList()

    fun countOnIsland(blockId: String, islandUuid: UUID): Int =
        byUuid.values.count { placed ->
            placed.blockId == blockId && islandUuidOf(placed) == islandUuid
        }

    fun register(placed: PlacedBlock) {
        index(placed)
        saveNow(placed)
    }

    fun unregister(location: SimpleLocation): PlacedBlock? {
        val removed = byLocation.remove(location) ?: return null
        byUuid.remove(removed.uuid)
        validated.remove(removed.uuid)
        warnedUnknownBlockId.remove(removed.uuid)
        synchronized(dirtyIds) { dirtyIds.remove(removed.uuid) }
        repository.remove(removed.uuid)
        return removed
    }

    fun isRegistered(placed: PlacedBlock): Boolean = byUuid[placed.uuid] === placed

    fun markDirty(placed: PlacedBlock) {
        if (!isRegistered(placed)) return
        synchronized(dirtyIds) { dirtyIds.add(placed.uuid) }
    }

    fun saveNow(placed: PlacedBlock) {
        if (!isRegistered(placed)) return
        synchronized(dirtyIds) { dirtyIds.remove(placed.uuid) }
        placed.syncStorage()
        repository.update(placed)
    }

    private fun index(placed: PlacedBlock) {
        byLocation[placed.location] = placed
        byUuid[placed.uuid] = placed
    }

    private fun islandUuidOf(placed: PlacedBlock): UUID? {
        val bukkit = placed.location.toBukkit() ?: return null
        return IslandManager.getIslandAt(bukkit)?.uuid
    }

    private fun takeDirtySnapshot(): List<PlacedBlock> {
        val ids = synchronized(dirtyIds) {
            if (dirtyIds.isEmpty()) return emptyList()
            val copy = dirtyIds.toList()
            dirtyIds.clear()
            copy
        }

        return ids.mapNotNull { byUuid[it] }.map { placed ->
            placed.syncStorage()
            placed.copy(
                upgrades = HashMap(placed.upgrades),
                storage = ArrayList(placed.storage),
                filter = HashSet(placed.filter),
            )
        }
    }

    private fun writeAll(blocks: List<PlacedBlock>) {
        if (blocks.isEmpty()) return
        runCatching { repository.updateAll(blocks) }
            .onFailure { Instance.logger.warning("[custom-items] flush de blocos falhou: ${it.message}") }
    }

    private fun flushDirty() {
        val snapshot = takeDirtySnapshot()
        if (snapshot.isEmpty()) return

        ScheduledExecutor.execute { writeAll(snapshot) }
    }

    private fun processTick() {
        tickCounter++

        byUuid.values.toList().forEach { placed ->
            val block = CustomBlockRegistry.get(placed.blockId) ?: run {
                warnUnknownBlockId(placed, "tick")
                return@forEach
            }
            if (!placed.enabled) return@forEach
            if (!isChunkLoaded(placed)) return@forEach
            if (!isStillValid(placed, block)) return@forEach

            val interval = block.tickInterval(placed).coerceAtLeast(1L)
            if (tickCounter % interval != 0L) return@forEach

            runCatching { block.tick(placed) }
                .onFailure { Instance.logger.warning("[custom-items] tick de '${placed.blockId}' falhou: ${it.message}") }
        }
    }

    private fun warnUnknownBlockId(placed: PlacedBlock, origin: String) {
        if (!warnedUnknownBlockId.add(placed.uuid)) return
        Instance.logger.warning(
            "[custom-items] ($origin) bloco ${placed.uuid} em ${placed.location} " +
                "tem blockId '${placed.blockId}' sem CustomBlock registrado."
        )
    }

    private fun isChunkLoaded(placed: PlacedBlock): Boolean {
        val bukkit: Location = placed.location.toBukkit() ?: return false
        val world = bukkit.world ?: return false
        return world.isChunkLoaded(bukkit.blockX shr 4, bukkit.blockZ shr 4)
    }

    private fun isStillValid(placed: PlacedBlock, custom: CustomBlock): Boolean {
        if (placed.uuid in validated) return true

        val bukkit = placed.location.toBukkit() ?: return false
        if (bukkit.block.type != custom.material && !custom.adopt(placed, bukkit.block)) {
            Instance.logger.warning(
                "[custom-items] bloco '${placed.blockId}' sumiu em ${placed.location}; registro removido."
            )
            unregister(placed.location)
            return false
        }

        validated.add(placed.uuid)
        return true
    }
}
